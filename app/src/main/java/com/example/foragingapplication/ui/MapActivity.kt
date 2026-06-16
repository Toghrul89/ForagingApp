package com.example.foragingapp.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.foragingapp.LogViewModel
import com.example.foragingapp.R
import com.example.foragingapp.auth.AuthManager
import com.example.foragingapp.databinding.ActivityMapBinding
import com.example.foragingapp.databinding.DialogAddTreeBinding
import com.example.foragingapp.model.LogEntry
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.IconFactory
import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.Property
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
    private val annotationLogIds = mutableMapOf<Long, Long>()
    private val viewModel: LogViewModel by viewModels()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var previewLog: LogEntry? = null

    companion object {
        private const val SOURCE_ID = "fruit-trees"
        private const val LAYER_ID = "fruit-trees-layer"
        private const val MARKER_IMAGE_ID = "tree-map-marker"
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

                val focusLat = intent.getDoubleExtra("FOCUS_LAT", Double.MIN_VALUE)
                val focusLng = intent.getDoubleExtra("FOCUS_LNG", Double.MIN_VALUE)
                val focusName = intent.getStringExtra("FOCUS_NAME")
                loadMarkersFromDatabase(focusLat, focusLng, focusName)

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
                    Toast.makeText(this, "Long-press to add a fruit discovery", Toast.LENGTH_LONG).show()
                }

                // Long-press to add (less accidental than single tap)
                mapLibreMap.addOnMapLongClickListener { point ->
                    if (AuthManager.isSignedIn(this)) {
                        showAddSpotDialog(point)
                    } else {
                        Toast.makeText(this, "Please sign in to contribute discoveries.", Toast.LENGTH_LONG).show()
                        startActivity(Intent(this, AuthActivity::class.java))
                    }
                    true
                }

                mapLibreMap.setOnMarkerClickListener { marker ->
                    showTreePreview(marker)
                    true
                }
            }
        }

        requestLocationPermission()
    }

    private fun setupMarkerLayer(style: Style) {
        style.addSource(GeoJsonSource(SOURCE_ID, FeatureCollection.fromFeatures(emptyList())))
        style.addImage(MARKER_IMAGE_ID, bitmapFromDrawable(R.drawable.ic_tree_map_marker))

        style.addLayer(
            org.maplibre.android.style.layers.CircleLayer("${LAYER_ID}-glow", SOURCE_ID)
                .withProperties(
                    circleRadius(24f),
                    circleColor("#D87916"),
                    circleOpacity(0.22f)
                )
        )

        style.addLayer(
            SymbolLayer(LAYER_ID, SOURCE_ID)
                .withProperties(
                    iconImage(MARKER_IMAGE_ID),
                    iconSize(0.95f),
                    iconAnchor(Property.ICON_ANCHOR_BOTTOM),
                    iconAllowOverlap(true),
                    iconIgnorePlacement(true)
                )
        )

        style.addLayer(
            SymbolLayer("${LAYER_ID}-label", SOURCE_ID)
                .withProperties(
                    textField("{name}"),
                    textSize(12f),
                    textOffset(arrayOf(0f, 1.2f)),
                    textAnchor(Property.TEXT_ANCHOR_TOP),
                    textColor("#1C2B1E"),
                    textHaloColor("#FFFFFF"),
                    textHaloWidth(2.5f),
                    textAllowOverlap(true)
                )
        )
    }

    private fun bitmapFromDrawable(drawableResId: Int): Bitmap {
        val drawable = requireNotNull(AppCompatResources.getDrawable(this, drawableResId)) {
            "Missing drawable resource $drawableResId"
        }
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 56
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 68
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    private fun refreshMarkers() {
        map?.style?.getSourceAs<GeoJsonSource>(SOURCE_ID)
            ?.setGeoJson(FeatureCollection.fromFeatures(markerFeatures))
        syncAnnotationMarkers()
    }

    private fun syncAnnotationMarkers() {
        val currentMap = map ?: return
        currentMap.clear()
        annotationLogIds.clear()

        val markerIcon = IconFactory.getInstance(this)
            .fromBitmap(bitmapFromDrawable(R.drawable.ic_tree_map_marker))

        markerFeatures.forEach { feature ->
            val point = feature.geometry() as? Point ?: return@forEach
            val name = feature.getStringProperty("name") ?: "Fruit tree"
            val marker = currentMap.addMarker(
                MarkerOptions()
                    .position(LatLng(point.latitude(), point.longitude()))
                    .title(name)
                    .icon(markerIcon)
            )
            val logId = feature.getNumberProperty("id")?.toLong() ?: -1L
            if (logId > 0) annotationLogIds[marker.id] = logId
        }
    }

    private fun openTreeDetails(marker: Marker) {
        val intent = Intent(this, TreeDetailsActivity::class.java)
        val logId = annotationLogIds[marker.id] ?: -1L
        if (logId > 0) {
            intent.putExtra("LOG_ID", logId)
        } else {
            intent.putExtra("TREE_NAME", marker.title)
            intent.putExtra("TREE_LAT", marker.position.latitude)
            intent.putExtra("TREE_LNG", marker.position.longitude)
        }
        startActivity(intent)
    }

    private fun showTreePreview(marker: Marker) {
        val logId = annotationLogIds[marker.id] ?: -1L
        if (logId <= 0) {
            binding.previewName.text = marker.title ?: "Fruit tree"
            binding.previewMeta.text = "Seattle discovery"
            binding.previewBadges.text = "Community Added • Access Unknown"
            binding.bottomPreview.visibility = View.VISIBLE
            binding.buttonPreviewDetails.setOnClickListener { openTreeDetails(marker) }
            binding.buttonPreviewSave.setOnClickListener {
                Toast.makeText(this, "Open details before saving this marker.", Toast.LENGTH_SHORT).show()
            }
            binding.buttonPreviewNavigate.setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=${marker.position.latitude},${marker.position.longitude}")))
            }
            binding.buttonPreviewShare.setOnClickListener {
                startActivity(
                    Intent.createChooser(
                        Intent(Intent.ACTION_SEND)
                            .setType("text/plain")
                            .putExtra(Intent.EXTRA_TEXT, "ZOGAL discovery: ${marker.title}\nhttps://maps.google.com/?q=${marker.position.latitude},${marker.position.longitude}"),
                        "Share discovery"
                    )
                )
            }
            return
        }
        lifecycleScope.launch {
            val log = viewModel.getLogById(logId) ?: return@launch
            previewLog = log
            binding.previewName.text = log.name
            binding.previewMeta.text = buildString {
                if (log.scientificName.isNotBlank()) append(log.scientificName)
                if (log.season.isNotBlank()) {
                    if (isNotBlank()) append(" • ")
                    append(log.season)
                }
                if (log.sourceLabel.isNotBlank()) {
                    if (isNotBlank()) append("\n")
                    append(log.sourceLabel)
                }
            }.ifBlank { log.location.ifBlank { "Seattle discovery" } }
            binding.previewBadges.text = "${sourceBadge(log)} • ${log.verificationStatus} • ${log.accessType}"
            binding.bottomPreview.visibility = View.VISIBLE
            binding.buttonPreviewDetails.setOnClickListener {
                startActivity(Intent(this@MapActivity, TreeDetailsActivity::class.java).putExtra("LOG_ID", log.id))
            }
            binding.buttonPreviewSave.setOnClickListener {
                if (AuthManager.isSignedIn(this@MapActivity)) {
                    viewModel.toggleFavorite(log)
                    Toast.makeText(this@MapActivity, "Saved", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MapActivity, "Please sign in to save trees.", Toast.LENGTH_LONG).show()
                    startActivity(Intent(this@MapActivity, AuthActivity::class.java))
                }
            }
            binding.buttonPreviewShare.setOnClickListener { shareTree(log) }
            binding.buttonPreviewNavigate.setOnClickListener { navigateToTree(log) }
        }
    }

    private fun sourceBadge(log: LogEntry): String {
        return if (log.dataSource == "OFFICIAL") "Official Dataset" else "Community Added"
    }

    private fun shareTree(log: LogEntry) {
        val coordinates = if (log.lat != null && log.lng != null) {
            "\nhttps://maps.google.com/?q=${log.lat},${log.lng}"
        } else {
            ""
        }
        startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND)
                    .setType("text/plain")
                    .putExtra(Intent.EXTRA_TEXT, "ZOGAL discovery: ${log.name}$coordinates"),
                "Share discovery"
            )
        )
    }

    private fun navigateToTree(log: LogEntry) {
        if (log.lat == null || log.lng == null) {
            Toast.makeText(this, "No coordinates for this discovery", Toast.LENGTH_SHORT).show()
            return
        }
        startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("google.navigation:q=${log.lat},${log.lng}")
            )
        )
    }

    private fun loadMarkersFromDatabase(
        focusLat: Double = Double.MIN_VALUE,
        focusLng: Double = Double.MIN_VALUE,
        focusName: String? = null
    ) {
        lifecycleScope.launch {
            markerFeatures.clear()
            viewModel.getAllLogsOnce().forEach { log ->
                if (log.lat != null && log.lng != null) {
                    markerFeatures.add(markerFeature(log.lng, log.lat, log.name, log.treeType, log.id))
                }
            }
            if (focusLat != Double.MIN_VALUE && focusLng != Double.MIN_VALUE) {
                val alreadyLoaded = markerFeatures.any { feature ->
                    val point = feature.geometry() as? Point
                    point != null &&
                        Math.abs(point.latitude() - focusLat) < 0.00001 &&
                        Math.abs(point.longitude() - focusLng) < 0.00001
                }
                if (!alreadyLoaded) {
                    markerFeatures.add(markerFeature(focusLng, focusLat, focusName ?: "Fruit tree", "Tree", null))
                }
            }
            refreshMarkers()
            if (markerFeatures.isEmpty()) {
                Toast.makeText(this@MapActivity, "No map discoveries yet", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this@MapActivity, "${markerFeatures.size} tree marker(s) loaded", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun markerFeature(lng: Double, lat: Double, name: String, treeType: String, id: Long?): Feature {
        val feature = Feature.fromGeometry(Point.fromLngLat(lng, lat))
        feature.addStringProperty("name", name)
        feature.addStringProperty("type", treeType)
        if (id != null) feature.addNumberProperty("id", id)
        return feature
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
        val user = AuthManager.currentUser(this)
        val entry = LogEntry(
            name = name,
            location = "Lat: ${String.format(Locale.US, "%.5f", point.latitude)}, Lng: ${String.format(Locale.US, "%.5f", point.longitude)}",
            date = date,
            notes = notes,
            lat = point.latitude,
            lng = point.longitude,
            creatorUserId = user?.id.orEmpty(),
            creatorName = user?.fullName.orEmpty(),
            createdAt = date,
            dataSource = "COMMUNITY",
            accessType = "Public",
            fruitCategory = "Fruit Tree",
            sourceLabel = user?.fullName?.takeIf { it.isNotBlank() }?.let { "Added by $it" }.orEmpty()
        )
        lifecycleScope.launch {
            val id = viewModel.insertAndReturnId(entry)
            markerFeatures.add(markerFeature(point.longitude, point.latitude, name, entry.treeType, id))
            refreshMarkers()
            Toast.makeText(this@MapActivity, "$name pinned", Toast.LENGTH_SHORT).show()
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
