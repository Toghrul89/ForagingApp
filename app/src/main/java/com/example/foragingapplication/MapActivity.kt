package com.example.foragingapplication

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.AnnotationConfig
import com.mapbox.maps.plugin.annotation.generated.PointAnnotation
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import com.mapbox.maps.plugin.lifecycle.*
import com.mapbox.maps.plugin.locationcomponent.location

class MapActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var pointAnnotationManager: PointAnnotationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        mapView = findViewById(R.id.mapView)

        mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS) {
            setupMarkerManager()
            requestLocationPermission()
        }
    }

    private fun setupMarkerManager() {
        val annotationApi = mapView.annotations
        pointAnnotationManager = annotationApi.createPointAnnotationManager(mapView, AnnotationConfig())

        mapView.gestures.addOnMapClickListener { point ->
            showAddTreeDialog(point)
            true
        }
    }

    private fun showAddTreeDialog(point: Point) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_tree, null)
        val nameInput = dialogView.findViewById<EditText>(R.id.treeNameInput)
        val noteInput = dialogView.findViewById<EditText>(R.id.treeNoteInput)

        AlertDialog.Builder(this)
            .setTitle("Add Tree")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val name = nameInput.text.toString().ifEmpty { "Unnamed Tree" }
                addTreeMarker(point, name)
                Toast.makeText(this, "$name added!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addTreeMarker(point: Point, title: String) {
        val marker = PointAnnotationOptions()
            .withPoint(point)
            .withTextField(title)
            .withTextSize(12.0)
        pointAnnotationManager.create(marker)
    }

    // Location permission logic
    private fun requestLocationPermission() {
        val permission = Manifest.permission.ACCESS_FINE_LOCATION
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), 1001)
        } else {
            enableUserLocation()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            enableUserLocation()
        } else {
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun enableUserLocation() {
        val locationComponentPlugin = mapView.location
        locationComponentPlugin.updateSettings {
            enabled = true
            pulsingEnabled = true
        }
    }

    // Lifecycle
    override fun onStart() { super.onStart(); mapView.onStart() }
    override fun onResume() { super.onResume(); mapView.onResume() }
    override fun onPause() { super.onPause(); mapView.onPause() }
    override fun onStop() { super.onStop(); mapView.onStop() }
    override fun onDestroy() { super.onDestroy(); mapView.onDestroy() }
}
