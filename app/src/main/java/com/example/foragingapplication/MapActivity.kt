package com.example.foragingapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.AnnotationConfig
import com.mapbox.maps.plugin.annotation.generated.PointAnnotation
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import com.mapbox.maps.plugin.lifecycle.onStart
import com.mapbox.maps.plugin.lifecycle.onStop
import com.mapbox.maps.plugin.locationcomponent.location

class MapActivity : AppCompatActivity() {
    private lateinit var mapView: MapView
    private lateinit var pointAnnotationManager: PointAnnotationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        mapView = findViewById(R.id.mapView)

        mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS) {
            val annotationApi = mapView.annotations
            pointAnnotationManager = annotationApi.createPointAnnotationManager(mapView, AnnotationConfig())

            mapView.gestures.addOnMapClickListener { point ->
                showAddTreeDialog(point)
                true
            }

            pointAnnotationManager.addClickListener { annotation: PointAnnotation ->
                val title = annotation.textField ?: "Fruit tree"
                val url = "https://en.wikipedia.org/wiki/Special:Search?search=" + Uri.encode(title)
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                true
            }
        }

        requestLocationPermission()
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
                addMarker(point, name)
                Toast.makeText(this, "$name added!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addMarker(point: Point, title: String) {
        val opts = PointAnnotationOptions()
            .withPoint(point)
            .withTextField(title)
            .withTextSize(12.0)
        pointAnnotationManager.create(opts)
    }

    private fun requestLocationPermission() {
        val perm = Manifest.permission.ACCESS_FINE_LOCATION
        if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(perm), 1001)
        } else enableUserLocation()
    }

    override fun onRequestPermissionsResult(req: Int, perms: Array<out String>, res: IntArray) {
        super.onRequestPermissionsResult(req, perms, res)
        if (req == 1001 && res.isNotEmpty() && res[0] == PackageManager.PERMISSION_GRANTED) enableUserLocation()
    }

    private fun enableUserLocation() {
        mapView.location.updateSettings { enabled = true; pulsingEnabled = true }
    }

    override fun onStart() { super.onStart(); mapView.onStart() }
    override fun onStop() { super.onStop(); mapView.onStop() }
}
