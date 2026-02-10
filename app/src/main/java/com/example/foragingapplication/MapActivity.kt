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
import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.AnnotationConfig
import com.mapbox.maps.plugin.annotation.generated.PointAnnotation
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import com.mapbox.maps.plugin.locationcomponent.location
import java.text.SimpleDateFormat
import java.util.*

class MapActivity : AppCompatActivity() {
    private lateinit var mapView: MapView
    private lateinit var pointAnnotationManager: PointAnnotationManager
    private lateinit var dbHelper: LogDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        dbHelper = LogDatabaseHelper(this)
        mapView = findViewById(R.id.mapView)

        mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS) {
            val annotationApi = mapView.annotations
            pointAnnotationManager = annotationApi.createPointAnnotationManager(mapView, AnnotationConfig())

            loadExistingMarkers()

            mapView.gestures.addOnMapClickListener { point ->
                showAddTreeDialog(point)
                true
            }

            pointAnnotationManager.addClickListener { annotation: PointAnnotation ->
                showMarkerDetails(annotation)
                true
            }
        }

        requestLocationPermission()
    }

    private fun loadExistingMarkers() {
        val logs = dbHelper.getAllLogs()
        
        for (log in logs) {
            if (log.lat != null && log.lng != null) {
                val point = Point.fromLngLat(log.lng, log.lat)
                addMarkerToMap(point, log.name, log.id)
            }
        }
        
        if (logs.isNotEmpty()) {
            val count = logs.count { it.lat != null && it.lng != null }
            Toast.makeText(this, "Loaded $count markers from database", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showAddTreeDialog(point: Point) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_tree, null)
        val nameInput = dialogView.findViewById<EditText>(R.id.treeNameInput)
        val noteInput = dialogView.findViewById<EditText>(R.id.treeNoteInput)

        AlertDialog.Builder(this)
            .setTitle("Add Tree at this Location")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val name = nameInput.text.toString().ifEmpty { "Unnamed Tree" }
                val notes = noteInput.text.toString()
                
                saveTreeToDatabase(name, notes, point)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveTreeToDatabase(name: String, notes: String, point: Point) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val currentDate = dateFormat.format(Date())
        
        val logEntry = LogEntry(
            name = name,
            location = "Lat: ${String.format("%.6f", point.latitude())}, Lng: ${String.format("%.6f", point.longitude())}",
            date = currentDate,
            notes = notes,
            lat = point.latitude(),
            lng = point.longitude()
        )
        
        val id = dbHelper.insertLog(logEntry)
        
        if (id > 0) {
            addMarkerToMap(point, name, id)
            Toast.makeText(this, "$name saved to database!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Failed to save tree", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addMarkerToMap(point: Point, title: String, logId: Long) {
        val opts = PointAnnotationOptions()
            .withPoint(point)
            .withTextField(title)
            .withTextSize(12.0)
        
        val annotation = pointAnnotationManager.create(opts)
        annotation.setData(com.google.gson.JsonPrimitive(logId))
    }

    private fun showMarkerDetails(annotation: PointAnnotation) {
        val logId = annotation.getData()?.asLong
        
        if (logId != null) {
            val log = dbHelper.getLogById(logId)
            
            if (log != null) {
                val message = """
                    Tree: ${log.name}
                    Location: ${log.location}
                    Date: ${log.date}
                    Notes: ${if (log.notes.isNotEmpty()) log.notes else "No notes"}
                """.trimIndent()
                
                AlertDialog.Builder(this)
                    .setTitle("Tree Details")
                    .setMessage(message)
                    .setPositiveButton("OK", null)
                    .setNegativeButton("Delete") { _, _ ->
                        deleteMarker(annotation, logId)
                    }
                    .show()
            }
        } else {
            val title = annotation.textField ?: "Unknown Tree"
            Toast.makeText(this, title, Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteMarker(annotation: PointAnnotation, logId: Long) {
        AlertDialog.Builder(this)
            .setTitle("Delete Tree?")
            .setMessage("Are you sure you want to delete this tree log?")
            .setPositiveButton("Delete") { _, _ ->
                val deleted = dbHelper.deleteLog(logId)
                
                if (deleted > 0) {
                    pointAnnotationManager.delete(annotation)
                    Toast.makeText(this, "Tree deleted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed to delete", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun requestLocationPermission() {
        val perm = Manifest.permission.ACCESS_FINE_LOCATION
        if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(perm), 1001)
        } else {
            enableUserLocation()
        }
    }

    override fun onRequestPermissionsResult(req: Int, perms: Array<out String>, res: IntArray) {
        super.onRequestPermissionsResult(req, perms, res)
        if (req == 1001 && res.isNotEmpty() && res[0] == PackageManager.PERMISSION_GRANTED) {
            enableUserLocation()
        }
    }

    private fun enableUserLocation() {
        mapView.location.updateSettings { 
            enabled = true
            pulsingEnabled = true
        }
    }

    override fun onStart() { 
        super.onStart()
        mapView.onStart()
    }
    
    override fun onStop() { 
        super.onStop()
        mapView.onStop()
    }
    
    override fun onDestroy() {
        dbHelper.close()
        super.onDestroy()
    }
}