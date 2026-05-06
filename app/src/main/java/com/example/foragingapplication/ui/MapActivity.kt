package com.example.foragingapp.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.foragingapp.LogViewModel
import com.example.foragingapp.R
import com.example.foragingapp.databinding.ActivityMapBinding
import com.example.foragingapp.databinding.DialogAddTreeBinding
import com.example.foragingapp.model.LogEntry
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.PropertyFactory.*
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import java.text.SimpleDateFormat
import java.util.*

class MapActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMapBinding
    private var map: MapLibreMap? = null
    private val markerFeatures = mutableListOf<Feature>()
    private val viewModel: LogViewModel by viewModels()
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    companion object {
        private const val SOURCE_ID = "fruit-trees"
        private const val LAYER_ID = "fruit-trees-layer"
        private const val LOCATION_PERMISSION_CODE = 1001
        private const val DEFAULT_LAT = 47.6062
        private const val DEFAULT_LNG = -122.3321
        private const val DEFAULT_ZOOM = 11.0
        private const val SPOT_ZOOM = 15.0

        /**
         * OpenFreeMap "bright" style — very stable, renders quickly.
         * Liberty style sometimes fails to load the sprite sheet which breaks
         * named icons like "marker-15". We use a style that works with custom
         * circle layers so we never depend on a remote sprite.
         */
        private const val MAP_STYLE =
            "https://tiles.openfreemap.org/styles/bright"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // MapLibre.getInstance MUST be called before setContentView
        MapLibre.getInstance(this)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        binding.mapView.onCreate(savedInstanceState)

        // Zoom controls
        binding.btnZoomIn.setOnClickListener {
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
        binding.btnZoomOut.setOnClickListener {
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

        // Locate-me button
        binding.btnLocateMe.setOnClickListener { zoomToMyLocation() }

        binding.mapView.getMapAsync { mapLibreMap ->
            map = mapLibreMap

            // Enable all gestures
            mapLibreMap.uiSettings.apply {
                isZoomGesturesEnabled = true
                isScrollGesturesEnabled = true
                isRotateGesturesEnabled = true
                isTiltGesturesEnabled = true
                isDoubleTapGesturesEnabled = true
                isQuickZoomGesturesEnabled = true
                // Keep attribution visible for publish readiness and tile provider compliance.
                isLogoEnabled = false
                isAttributionEnabled = true
            }

            mapLibreMap.setStyle(MAP_STYLE) { style ->
                setupMarkerLayer(style)
                loadMarkersFromDatabase()

                val focusLat = intent.getDoubleExtra("FOCUS_LAT", Double.MIN_VALUE)
                val focusLng = intent.getDoubleExtra("FOCUS_LNG", Double.MIN_VALUE)
                val focusName = intent.getStringExtra("FOCUS_NAME")

                if (focusLat != Double.MIN_VALUE && focusLng != Double.MIN_VALUE) {
                    mapLibreMap.animateCamera(
                        CameraUpdateFactory.newCameraPosition(
                            CameraPosition.Builder()
                                .target(LatLng(focusLat, focusLng))
                                .zoom(SPOT_ZOOM)
                                .build()
                        ), 800
                    )
                    if (focusName != null) {
                        binding.toolbar.title = focusName
                        Toast.makeText(this, "Showing: $focusName", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    mapLibreMap.animateCamera(
                        CameraUpdateFactory.newCameraPosition(
                            CameraPosition.Builder()
                                .target(LatLng(DEFAULT_LAT, DEFAULT_LNG))
                                .zoom(DEFAULT_ZOOM)
                                .build()
                        ), 500
                    )
                    Toast.makeText(this, "Long-press anywhere to pin a tree", Toast.LENGTH_LONG).show()
                }

                // Long-press to add (less accidental than single tap)
                mapLibreMap.addOnMapLongClickListener { point ->
                    showAddSpotDialog(point)
                    true
                }
            }
        }

        requestLocationPermission()
    }

    /**
     * Use a CircleLayer instead of SymbolLayer so we have zero dependency
     * on a remote sprite sheet. This is the primary fix for the "map not
     * working good" issue — sprite-load failures silently suppress all markers.
     */
    private fun setupMarkerLayer(style: Style) {
        style.addSource(GeoJsonSource(SOURCE_ID, FeatureCollection.fromFeatures(emptyList())))

        // Outer glow
        style.addLayer(
            org.maplibre.android.style.layers.CircleLayer("${LAYER_ID}-glow", SOURCE_ID)
                .withProperties(
                    circleRadius(18f),
                    circleColor("#4A7C52"),
                    circleOpacity(0.25f)
                )
        )

        // Main dot
        style.addLayer(
            org.maplibre.android.style.layers.CircleLayer(LAYER_ID, SOURCE_ID)
                .withProperties(
                    circleRadius(10f),
                    circleColor("#2C4A2E"),
                    circleStrokeWidth(2.5f),
                    circleStrokeColor("#FFFFFF")
                )
        )

        // Label below the dot
        style.addLayer(
            SymbolLayer("${LAYER_ID}-label", SOURCE_ID)
                .withProperties(
                    textField("{name}"),
                    textSize(11f),
                    textOffset(arrayOf(0f, 2.2f)),
                    textColor("#1C2B1E"),
                    textHaloColor("#FFFFFF"),
                    textHaloWidth(2f),
                    textAllowOverlap(false),
                    iconAllowOverlap(true)
                )
        )
    }

    private fun refreshMarkers() {
        map?.style?.getSourceAs<GeoJsonSource>(SOURCE_ID)
            ?.setGeoJson(FeatureCollection.fromFeatures(markerFeatures))
    }

    private fun loadMarkersFromDatabase() {
        lifecycleScope.launch {
            markerFeatures.clear()
            viewModel.getAllLogsOnce().forEach { log ->
                if (log.lat != null && log.lng != null) {
                    val feature = Feature.fromGeometry(Point.fromLngLat(log.lng, log.lat))
                    feature.addStringProperty("name", log.name)
                    feature.addStringProperty("type", log.treeType)
                    markerFeatures.add(feature)
                }
            }
            refreshMarkers()
        }
    }

    private fun showAddSpotDialog(point: LatLng) {
        val dialogBinding = DialogAddTreeBinding.inflate(LayoutInflater.from(this))
        AlertDialog.Builder(this)
            .setTitle("Pin a fruit tree")
            .setView(dialogBinding.root)
            .setPositiveButton("Save") { _, _ ->
                val name = dialogBinding.treeNameInput.text?.toString()?.trim()
                    .takeIf { !it.isNullOrEmpty() } ?: "Unnamed tree"
                val notes = dialogBinding.treeNoteInput.text?.toString()?.trim() ?: ""
                saveSpot(name, notes, point)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveSpot(name: String, notes: String, point: LatLng) {
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        val entry = LogEntry(
            name = name,
            location = "Lat: ${String.format(Locale.US, "%.5f", point.latitude)}, Lng: ${String.format(Locale.US, "%.5f", point.longitude)}",
            date = date,
            notes = notes,
            lat = point.latitude,
            lng = point.longitude
        )
        lifecycleScope.launch {
            viewModel.insert(entry)
            val feature = Feature.fromGeometry(Point.fromLngLat(point.longitude, point.latitude))
            feature.addStringProperty("name", name)
            markerFeatures.add(feature)
            refreshMarkers()
            Toast.makeText(this@MapActivity, "🌿 $name pinned!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun zoomToMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestLocationPermission()
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
            if (loc != null) {
                map?.animateCamera(
                    CameraUpdateFactory.newCameraPosition(
                        CameraPosition.Builder()
                            .target(LatLng(loc.latitude, loc.longitude))
                            .zoom(SPOT_ZOOM)
                            .build()
                    ), 600
                )
            } else {
                Toast.makeText(this, "Cannot get your location right now", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "Location permission granted", Toast.LENGTH_SHORT).show()
        }
    }

    // MapLibre lifecycle — all required
    override fun onStart()  { super.onStart();  binding.mapView.onStart() }
    override fun onResume() { super.onResume(); binding.mapView.onResume(); loadMarkersFromDatabase() }
    override fun onPause()  { super.onPause();  binding.mapView.onPause() }
    override fun onStop()   { super.onStop();   binding.mapView.onStop() }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState); binding.mapView.onSaveInstanceState(outState)
    }
    override fun onLowMemory() { super.onLowMemory(); binding.mapView.onLowMemory() }
    override fun onDestroy()   { super.onDestroy();   binding.mapView.onDestroy() }
}
