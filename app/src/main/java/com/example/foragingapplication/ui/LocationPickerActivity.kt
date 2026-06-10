package com.example.foragingapp.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.foragingapp.R
import com.example.foragingapp.databinding.ActivityLocationPickerBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.IconFactory
import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import java.util.Locale

class LocationPickerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLocationPickerBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var map: MapLibreMap? = null
    private var selectedMarker: Marker? = null
    private var selectedPoint: LatLng? = null

    companion object {
        const val EXTRA_LAT = "EXTRA_LAT"
        const val EXTRA_LNG = "EXTRA_LNG"
        const val EXTRA_LOCATION_LABEL = "EXTRA_LOCATION_LABEL"
        private const val LOCATION_PERMISSION_CODE = 3001
        private const val DEFAULT_LAT = 47.6062
        private const val DEFAULT_LNG = -122.3321
        private const val DEFAULT_ZOOM = 12.0
        private const val PICKER_ZOOM = 15.0
        private const val MAP_STYLE = "https://tiles.openfreemap.org/styles/bright"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapLibre.getInstance(this)
        binding = ActivityLocationPickerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        binding.mapView.onCreate(savedInstanceState)

        binding.btnZoomIn.setOnClickListener { zoomBy(1.0) }
        binding.btnZoomOut.setOnClickListener { zoomBy(-1.0) }
        binding.btnLocateMe.setOnClickListener { centerNearUserOrSeattle() }
        binding.buttonUseLocation.setOnClickListener { returnSelectedLocation() }

        binding.mapView.getMapAsync { mapLibreMap ->
            map = mapLibreMap
            mapLibreMap.uiSettings.apply {
                isZoomGesturesEnabled = true
                isScrollGesturesEnabled = true
                isRotateGesturesEnabled = true
                isTiltGesturesEnabled = true
                isDoubleTapGesturesEnabled = true
                isQuickZoomGesturesEnabled = true
                isLogoEnabled = false
                isAttributionEnabled = true
            }

            mapLibreMap.setStyle(MAP_STYLE) {
                val initialLat = intent.getDoubleExtra(EXTRA_LAT, Double.MIN_VALUE)
                val initialLng = intent.getDoubleExtra(EXTRA_LNG, Double.MIN_VALUE)
                if (initialLat != Double.MIN_VALUE && initialLng != Double.MIN_VALUE) {
                    val point = LatLng(initialLat, initialLng)
                    moveCamera(point, PICKER_ZOOM)
                    placeMarker(point)
                } else {
                    moveCamera(LatLng(DEFAULT_LAT, DEFAULT_LNG), DEFAULT_ZOOM)
                    requestLocationPermissionIfNeeded()
                    centerNearUserOrSeattle()
                }
            }

            mapLibreMap.addOnMapClickListener { point ->
                placeMarker(point)
                true
            }
        }
    }

    private fun placeMarker(point: LatLng) {
        selectedPoint = point
        val currentMap = map ?: return
        selectedMarker?.remove()
        selectedMarker = currentMap.addMarker(
            MarkerOptions()
                .position(point)
                .title("Selected tree location")
                .icon(IconFactory.getInstance(this).fromBitmap(markerBitmap()))
        )
        binding.buttonUseLocation.isEnabled = true
        binding.textHint.text = "Selected: ${formatLatLng(point)}"
    }

    private fun markerBitmap(): Bitmap {
        val drawable = requireNotNull(AppCompatResources.getDrawable(this, R.drawable.ic_tree_map_marker))
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 56
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 68
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    private fun centerNearUserOrSeattle() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestLocationPermissionIfNeeded()
            moveCamera(LatLng(DEFAULT_LAT, DEFAULT_LNG), DEFAULT_ZOOM)
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            val point = if (location != null) {
                LatLng(location.latitude, location.longitude)
            } else {
                LatLng(DEFAULT_LAT, DEFAULT_LNG)
            }
            moveCamera(point, if (location != null) PICKER_ZOOM else DEFAULT_ZOOM)
        }.addOnFailureListener {
            moveCamera(LatLng(DEFAULT_LAT, DEFAULT_LNG), DEFAULT_ZOOM)
        }
    }

    private fun moveCamera(point: LatLng, zoom: Double) {
        map?.animateCamera(
            CameraUpdateFactory.newCameraPosition(
                CameraPosition.Builder()
                    .target(point)
                    .zoom(zoom)
                    .build()
            ),
            500
        )
    }

    private fun zoomBy(delta: Double) {
        val currentMap = map ?: return
        currentMap.animateCamera(
            CameraUpdateFactory.newCameraPosition(
                CameraPosition.Builder()
                    .target(currentMap.cameraPosition.target)
                    .zoom(currentMap.cameraPosition.zoom + delta)
                    .build()
            ),
            250
        )
    }

    private fun returnSelectedLocation() {
        val point = selectedPoint ?: return
        val label = "Lat: ${formatCoordinate(point.latitude)}, Lng: ${formatCoordinate(point.longitude)}"
        setResult(
            Activity.RESULT_OK,
            Intent()
                .putExtra(EXTRA_LAT, point.latitude)
                .putExtra(EXTRA_LNG, point.longitude)
                .putExtra(EXTRA_LOCATION_LABEL, label)
        )
        finish()
    }

    private fun requestLocationPermissionIfNeeded() {
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

    private fun formatLatLng(point: LatLng): String {
        return "${formatCoordinate(point.latitude)}, ${formatCoordinate(point.longitude)}"
    }

    private fun formatCoordinate(value: Double): String {
        return String.format(Locale.US, "%.5f", value)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_CODE &&
            grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED
        ) {
            centerNearUserOrSeattle()
        }
    }

    override fun onStart() { super.onStart(); binding.mapView.onStart() }
    override fun onResume() { super.onResume(); binding.mapView.onResume() }
    override fun onPause() { super.onPause(); binding.mapView.onPause() }
    override fun onStop() { super.onStop(); binding.mapView.onStop() }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.mapView.onSaveInstanceState(outState)
    }
    override fun onLowMemory() { super.onLowMemory(); binding.mapView.onLowMemory() }
    override fun onDestroy() { super.onDestroy(); binding.mapView.onDestroy() }
}
