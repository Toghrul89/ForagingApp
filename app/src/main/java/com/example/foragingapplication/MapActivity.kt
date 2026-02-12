package com.example.foragingapp

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
import com.example.foragingapp.data.LogDatabaseHelper
import com.example.foragingapp.model.LogEntry
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.Style
import java.text.SimpleDateFormat
import java.util.*

class MapActivity : AppCompatActivity() {
    private lateinit var mapView: MapView
    private lateinit var dbHelper: LogDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Mapbox
        Mapbox.getInstance(this, null)
        
        setContentView(R.layout.activity_map)

        dbHelper = LogDatabaseHelper(this)
        mapView = findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)

        mapView.getMapAsync { map ->
            map.setStyle(Style.MAPBOX_STREETS) {
                
                // Set initial camera position
                val position = CameraPosition.Builder()
                    .target(LatLng(40.7128, -74.0060)) // New York
                    .zoom(10.0)
                    .build()
                map.cameraPosition = position
                
                Toast.makeText(this, "Map loaded! Tap anywhere to add a tree.", Toast.LENGTH_LONG).show()
                
                // Add click listener
                map.addOnMapClickListener { point ->
                    showAddTreeDialog(point)
                    true
                }
            }
        }

        requestLocationPermission()
    }

    private fun showAddTreeDialog(point: LatLng) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_tree, null)
        val nameInput = dialogView.findViewById<EditText>(R.id.treeNameInput)
        val noteInput = dialogView.findViewById<EditText>(R.id.treeNoteInput)

        AlertDialog.Builder(this)
            .setTitle("Add Tree at Location")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val name = nameInput.text.toString().ifEmpty { "Unnamed Tree" }
                val notes = noteInput.text.toString()
                saveTreeToDatabase(name, notes, point)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveTreeToDatabase(name: String, notes: String, point: LatLng) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val currentDate = dateFormat.format(Date())
        
        val logEntry = LogEntry(
            name = name,
            location = "Lat: ${String.format("%.6f", point.latitude)}, Lng: ${String.format("%.6f", point.longitude)}",
            date = currentDate,
            notes = notes,
            lat = point.latitude,
            lng = point.longitude
        )
        
        val id = dbHelper.insertLog(logEntry)
        
        if (id > 0) {
            Toast.makeText(this, "$name saved to database!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Failed to save tree", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestLocationPermission() {
        val perm = Manifest.permission.ACCESS_FINE_LOCATION
        if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(perm), 1001)
        }
    }

    override fun onRequestPermissionsResult(req: Int, perms: Array<out String>, res: IntArray) {
        super.onRequestPermissionsResult(req, perms, res)
        if (req == 1001 && res.isNotEmpty() && res[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Location permission granted", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
        dbHelper.close()
    }
}