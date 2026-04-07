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
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import java.text.SimpleDateFormat
import java.util.*

class MapActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var dbHelper: LogDatabaseHelper
    private var map: MapLibreMap? = null
    private val markerFeatures = mutableListOf<Feature>()

    companion object {
        private const val SOURCE_ID = "foraging-spots"
        private const val LAYER_ID = "foraging-spots-layer"
        private const val LOCATION_PERMISSION_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapLibre.getInstance(this)
        setContentView(R.layout.activity_map)

        dbHelper = LogDatabaseHelper(this)
        mapView = findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)

        mapView.getMapAsync { mapLibreMap ->
            map = mapLibreMap
            mapLibreMap.setStyle("https://demotiles.maplibre.org/style.json") { style ->
                setupMarkerLayer(style)
                loadMarkersFromDatabase()
                centerMap()
                Toast.makeText(this, "Tap anywhere to add a spot!", Toast.LENGTH_LONG).show()
                mapLibreMap.addOnMapClickListener { point ->
                    showAddSpotDialog(point)
                    true
                }
            }
        }

        requestLocationPermission()
    }

    // Sets up a GeoJSON source + SymbolLayer for markers
    private fun setupMarkerLayer(style: Style) {
        style.addSource(GeoJsonSource(SOURCE_ID, FeatureCollection.fromFeatures(emptyList())))
        style.addLayer(
            SymbolLayer(LAYER_ID, SOURCE_ID).withProperties(
                PropertyFactory.textField("{name}"),
                PropertyFactory.textSize(13f),
                PropertyFactory.textOffset(arrayOf(0f, 1.5f)),
                PropertyFactory.iconImage("marker"),
                PropertyFactory.iconAllowOverlap(true),
                PropertyFactory.textAllowOverlap(true)
            )
        )
    }

    // Refreshes the GeoJSON source with all current markers
    private fun refreshMarkers() {
        val source = map?.style?.getSourceAs<GeoJsonSource>(SOURCE_ID)
        source?.setGeoJson(FeatureCollection.fromFeatures(markerFeatures))
    }

    private fun loadMarkersFromDatabase() {
        val logs = dbHelper.getAllLogs()
        for (log in logs) {
            if (log.lat != null && log.lng != null) {
                val feature = Feature.fromGeometry(Point.fromLngLat(log.lng, log.lat))
                feature.addStringProperty("name", log.name)
                markerFeatures.add(feature)
            }
        }
        refreshMarkers()
    }

    private fun centerMap() {
        val logsWithCoords = dbHelper.getAllLogs().filter { it.lat != null && it.lng != null }
        val target = if (logsWithCoords.isNotEmpty()) {
            LatLng(logsWithCoords.first().lat!!, logsWithCoords.first().lng!!)
        } else {
            LatLng(47.6062, -122.3321) // Seattle fallback
        }
        map?.cameraPosition = CameraPosition.Builder().target(target).zoom(10.0).build()
    }

    private fun showAddSpotDialog(point: LatLng) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_tree, null)
        val nameInput = dialogView.findViewById<EditText>(R.id.treeNameInput)
        val noteInput = dialogView.findViewById<EditText>(R.id.treeNoteInput)

        AlertDialog.Builder(this)
            .setTitle("Add Spot Here")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val name = nameInput.text.toString().ifEmpty { "Unnamed Spot" }
                val notes = noteInput.text.toString()
                saveSpot(name, notes, point)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveSpot(name: String, notes: String, point: LatLng) {
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val entry = LogEntry(
            name = name,
            location = "Lat: ${String.format("%.6f", point.latitude)}, Lng: ${String.format("%.6f", point.longitude)}",
            date = date,
            notes = notes,
            lat = point.latitude,
            lng = point.longitude
        )
        val id = dbHelper.insertLog(entry)
        if (id > 0) {
            // Add pin immediately without reloading everything
            val feature = Feature.fromGeometry(Point.fromLngLat(point.longitude, point.latitude))
            feature.addStringProperty("name", name)
            markerFeatures.add(feature)
            refreshMarkers()
            Toast.makeText(this, "$name saved!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Failed to save", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_CODE && grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Location permission granted", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onStart() { super.onStart(); mapView.onStart() }
    override fun onResume() { super.onResume(); mapView.onResume() }
    override fun onPause() { super.onPause(); mapView.onPause() }
    override fun onStop() { super.onStop(); mapView.onStop() }
    override fun onSaveInstanceState(outState: Bundle) { super.onSaveInstanceState(outState); mapView.onSaveInstanceState(outState) }
    override fun onLowMemory() { super.onLowMemory(); mapView.onLowMemory() }
    override fun onDestroy() { super.onDestroy(); mapView.onDestroy(); dbHelper.close() }
}