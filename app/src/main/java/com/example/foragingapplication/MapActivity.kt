package com.example.foragingapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.foragingapp.model.LogEntry
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.launch
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
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
    private var map: MapLibreMap? = null
    private val markerFeatures = mutableListOf<Feature>()

    private val viewModel: LogViewModel by viewModels()

    companion object {
        private const val SOURCE_ID = "foraging-spots"
        private const val LAYER_ID = "foraging-spots-layer"
        private const val LOCATION_PERMISSION_CODE = 1001
        private const val SEATTLE_LAT = 47.6062
        private const val SEATTLE_LNG = -122.3321
        private const val DEFAULT_ZOOM = 11.0
        private const val SPOT_ZOOM = 15.0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapLibre.getInstance(this)
        setContentView(R.layout.activity_map)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        mapView = findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)

        findViewById<TextView>(R.id.btnZoomIn).setOnClickListener {
            map?.let { m ->
                m.animateCamera(
                    CameraUpdateFactory.newCameraPosition(
                        CameraPosition.Builder()
                            .target(m.cameraPosition.target)
                            .zoom(m.cameraPosition.zoom + 1.0)
                            .build()
                    ), 300
                )
            }
        }

        findViewById<TextView>(R.id.btnZoomOut).setOnClickListener {
            map?.let { m ->
                m.animateCamera(
                    CameraUpdateFactory.newCameraPosition(
                        CameraPosition.Builder()
                            .target(m.cameraPosition.target)
                            .zoom(m.cameraPosition.zoom - 1.0)
                            .build()
                    ), 300
                )
            }
        }

        mapView.getMapAsync { mapLibreMap ->
            map = mapLibreMap

            mapLibreMap.uiSettings.apply {
                isZoomGesturesEnabled = true
                isScrollGesturesEnabled = true
                isRotateGesturesEnabled = true
                isTiltGesturesEnabled = true
                isDoubleTapGesturesEnabled = true
                isQuickZoomGesturesEnabled = true
            }

            mapLibreMap.setStyle("https://tiles.openfreemap.org/styles/liberty") { style ->
                setupMarkerLayer(style)
                loadMarkersFromDatabase()

                val focusLat  = intent.getDoubleExtra("FOCUS_LAT", Double.MIN_VALUE)
                val focusLng  = intent.getDoubleExtra("FOCUS_LNG", Double.MIN_VALUE)
                val focusName = intent.getStringExtra("FOCUS_NAME")

                if (focusLat != Double.MIN_VALUE && focusLng != Double.MIN_VALUE) {
                    mapLibreMap.cameraPosition = CameraPosition.Builder()
                        .target(LatLng(focusLat, focusLng))
                        .zoom(SPOT_ZOOM)
                        .build()
                    if (focusName != null) {
                        toolbar.title = focusName
                        Toast.makeText(this, "Showing: $focusName", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    mapLibreMap.cameraPosition = CameraPosition.Builder()
                        .target(LatLng(SEATTLE_LAT, SEATTLE_LNG))
                        .zoom(DEFAULT_ZOOM)
                        .build()
                    Toast.makeText(this, "Tap anywhere to pin a new spot", Toast.LENGTH_LONG).show()
                }

                mapLibreMap.addOnMapClickListener { point ->
                    showAddSpotDialog(point)
                    true
                }
            }
        }

        requestLocationPermission()
    }

    private fun setupMarkerLayer(style: Style) {
        style.addSource(GeoJsonSource(SOURCE_ID, FeatureCollection.fromFeatures(emptyList())))
        style.addLayer(
            SymbolLayer(LAYER_ID, SOURCE_ID).withProperties(
                PropertyFactory.textField("{name}"),
                PropertyFactory.textSize(12f),
                PropertyFactory.textOffset(arrayOf(0f, 1.8f)),
                PropertyFactory.textColor("#1B5E20"),
                PropertyFactory.textHaloColor("#FFFFFF"),
                PropertyFactory.textHaloWidth(2f),
                PropertyFactory.iconImage("marker-15"),
                PropertyFactory.iconAllowOverlap(true),
                PropertyFactory.textAllowOverlap(true)
            )
        )
    }

    private fun refreshMarkers() {
        val source = map?.style?.getSourceAs<GeoJsonSource>(SOURCE_ID)
        source?.setGeoJson(FeatureCollection.fromFeatures(markerFeatures))
    }

    private fun loadMarkersFromDatabase() {
        lifecycleScope.launch {
            markerFeatures.clear()
            val logs = viewModel.getAllLogsOnce()
            for (log in logs) {
                if (log.lat != null && log.lng != null) {
                    val feature = Feature.fromGeometry(Point.fromLngLat(log.lng, log.lat))
                    feature.addStringProperty("name", log.name)
                    markerFeatures.add(feature)
                }
            }
            refreshMarkers()
        }
    }

    private fun showAddSpotDialog(point: LatLng) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_tree, null)
        val nameInput  = dialogView.findViewById<EditText>(R.id.treeNameInput)
        val noteInput  = dialogView.findViewById<EditText>(R.id.treeNoteInput)

        AlertDialog.Builder(this)
            .setTitle("Add Spot Here")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val name  = nameInput.text.toString().ifEmpty { "Unnamed Spot" }
                val notes = noteInput.text.toString()
                saveSpot(name, notes, point)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveSpot(name: String, notes: String, point: LatLng) {
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        val entry = LogEntry(
            name     = name,
            location = "Lat: ${String.format(Locale.US, "%.5f", point.latitude)}, Lng: ${String.format(Locale.US, "%.5f", point.longitude)}",
            date     = date,
            notes    = notes,
            lat      = point.latitude,
            lng      = point.longitude
        )
        lifecycleScope.launch {
            viewModel.insert(entry)
            val feature = Feature.fromGeometry(Point.fromLngLat(point.longitude, point.latitude))
            feature.addStringProperty("name", name)
            markerFeatures.add(feature)
            refreshMarkers()
            Toast.makeText(this@MapActivity, "$name saved!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "Location permission granted", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onStart()    { super.onStart();    mapView.onStart() }
    override fun onResume()   { super.onResume();   mapView.onResume() }
    override fun onPause()    { super.onPause();    mapView.onPause() }
    override fun onStop()     { super.onStop();     mapView.onStop() }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }
    override fun onLowMemory() { super.onLowMemory(); mapView.onLowMemory() }
    override fun onDestroy()   { super.onDestroy();   mapView.onDestroy() }
}