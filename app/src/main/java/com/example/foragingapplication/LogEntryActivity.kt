package com.example.foragingapp

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.foragingapp.data.LogDatabaseHelper
import com.example.foragingapp.model.LogEntry
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class LogEntryActivity : AppCompatActivity() {

    private lateinit var dbHelper: LogDatabaseHelper
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var currentPhotoUri: Uri? = null
    private var currentLat: Double? = null
    private var currentLng: Double? = null
    private var editingLogId: Long? = null

    private lateinit var editTextTreeName: EditText
    private lateinit var editTextLocation: EditText
    private lateinit var editTextNotes: EditText
    private lateinit var buttonSave: Button
    private lateinit var buttonGetLocation: Button
    private lateinit var buttonSelectImage: Button
    private lateinit var buttonTakePhoto: Button
    private lateinit var imageViewPreview: ImageView
    private lateinit var tvCoordinates: TextView

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            currentPhotoUri = it
            imageViewPreview.setImageURI(it)
            imageViewPreview.visibility = View.VISIBLE
        }
    }

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            currentPhotoUri?.let {
                imageViewPreview.setImageURI(it)
                imageViewPreview.visibility = View.VISIBLE
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log_entry)

        // Toolbar with back button
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        dbHelper = LogDatabaseHelper(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        editTextTreeName = findViewById(R.id.editTextTreeName)
        editTextLocation = findViewById(R.id.editTextLocation)
        editTextNotes = findViewById(R.id.editTextNotes)
        buttonSave = findViewById(R.id.buttonSaveLog)
        buttonGetLocation = findViewById(R.id.buttonGetLocation)
        buttonSelectImage = findViewById(R.id.buttonSelectImage)
        buttonTakePhoto = findViewById(R.id.buttonTakePhoto)
        imageViewPreview = findViewById(R.id.imageViewPreview)
        tvCoordinates = findViewById(R.id.tvCoordinates)

        editingLogId = intent.getLongExtra("LOG_ID", -1).takeIf { it != -1L }
        if (editingLogId != null) {
            loadExistingLog(editingLogId!!)
            toolbar.title = "Edit Spot"
            buttonSave.text = "💾 Update Spot"
        }

        buttonGetLocation.setOnClickListener { getCurrentLocation() }
        buttonSelectImage.setOnClickListener { pickImageLauncher.launch("image/*") }
        buttonTakePhoto.setOnClickListener { takePhoto() }
        buttonSave.setOnClickListener { saveLog() }
    }

    private fun loadExistingLog(logId: Long) {
        val log = dbHelper.getLogById(logId) ?: return
        editTextTreeName.setText(log.name)
        editTextLocation.setText(log.location)
        editTextNotes.setText(log.notes)
        currentLat = log.lat
        currentLng = log.lng

        if (log.lat != null && log.lng != null) {
            tvCoordinates.text = "📍 Lat: ${String.format("%.6f", log.lat)}, Lng: ${String.format("%.6f", log.lng)}"
            tvCoordinates.visibility = View.VISIBLE
        }

        if (log.imageUri.isNotEmpty()) {
            currentPhotoUri = Uri.parse(log.imageUri)
            imageViewPreview.setImageURI(currentPhotoUri)
            imageViewPreview.visibility = View.VISIBLE
        }
    }

    private fun getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                currentLat = location.latitude
                currentLng = location.longitude
                tvCoordinates.text = "📍 Lat: ${String.format("%.6f", location.latitude)}, Lng: ${String.format("%.6f", location.longitude)}"
                tvCoordinates.visibility = View.VISIBLE
                if (editTextLocation.text.isEmpty()) {
                    editTextLocation.setText("${String.format("%.5f", location.latitude)}, ${String.format("%.5f", location.longitude)}")
                }
                Toast.makeText(this, "Location captured!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Unable to get location. Try again.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun takePhoto() {
        val photoFile = File(getExternalFilesDir(null), "IMG_${System.currentTimeMillis()}.jpg")
        currentPhotoUri = FileProvider.getUriForFile(this, "${applicationContext.packageName}.fileprovider", photoFile)
        takePictureLauncher.launch(currentPhotoUri)
    }

    private fun saveLog() {
        val treeName = editTextTreeName.text?.toString()?.trim()
        val location = editTextLocation.text?.toString()?.trim()
        val notes = editTextNotes.text?.toString()?.trim() ?: ""

        if (treeName.isNullOrEmpty()) {
            editTextTreeName.error = "Spot name is required"
            editTextTreeName.requestFocus()
            return
        }
        if (location.isNullOrEmpty()) {
            editTextLocation.error = "Location is required"
            editTextLocation.requestFocus()
            return
        }

        val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        val logEntry = LogEntry(
            id = editingLogId ?: 0,
            name = treeName,
            location = location,
            date = date,
            notes = notes,
            imageUri = currentPhotoUri?.toString() ?: "",
            lat = currentLat,
            lng = currentLng
        )

        val success = if (editingLogId != null) {
            dbHelper.updateLog(logEntry) > 0
        } else {
            dbHelper.insertLog(logEntry) > 0
        }

        if (success) {
            Toast.makeText(this, "✅ Spot saved!", Toast.LENGTH_SHORT).show()
            finish()
        } else {
            Toast.makeText(this, "Failed to save. Try again.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE &&
            grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation()
        } else {
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        dbHelper.close()
        super.onDestroy()
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1002
    }
}