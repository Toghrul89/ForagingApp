package com.example.foragingapp.ui

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.foragingapp.LogViewModel
import com.example.foragingapp.databinding.ActivityLogEntryBinding
import com.example.foragingapp.model.LogEntry
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class LogEntryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLogEntryBinding
    private val viewModel: LogViewModel by viewModels()
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var currentPhotoUri: Uri? = null
    private var currentLat: Double? = null
    private var currentLng: Double? = null
    private var editingEntry: LogEntry? = null

    companion object {
        private const val LOC_PERMISSION_CODE = 1002

        // Zogal (Cornelian Cherry) is listed first as the hero fruit
        val TREE_TYPES = listOf(
            "Cornelian Cherry 🌿", // Zogal
            "Apple 🍎",
            "Cherry 🍒",
            "Pear 🍐",
            "Plum 🫐",
            "Mulberry 🫐",
            "Fig 🍈",
            "Walnut 🌰",
            "Hazelnut 🌰",
            "Blackberry 🫐",
            "Elderberry",
            "Quince",
            "Persimmon",
            "Crabapple",
            "Hawthorn",
            "Serviceberry",
            "Other 🌿"
        )

        val SEASONS = listOf("Spring", "Summer", "Autumn", "Winter", "Year-round")

        val RIPENESS_OPTIONS = listOf(
            "🌱 Unripe",
            "🟡 Almost Ready",
            "🔴 Peak — harvest now!",
            "🍂 Overripe"
        )
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { loadPhoto(it) } }

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success -> if (success) currentPhotoUri?.let { loadPhoto(it) } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLogEntryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setupSpinners()

        val editingId = intent.getLongExtra("LOG_ID", -1).takeIf { it != -1L }
        if (editingId != null) {
            lifecycleScope.launch {
                viewModel.getLogById(editingId)?.let { log ->
                    editingEntry = log
                    populateFields(log)
                    supportActionBar?.title = "Edit Spot"
                    binding.buttonSaveLog.text = "UPDATE\nSPOT"
                }
            }
        } else {
            supportActionBar?.title = "New Spot"
        }

        binding.buttonGetLocation.setOnClickListener { getCurrentLocation() }
        binding.buttonSelectImage.setOnClickListener { pickImageLauncher.launch("image/*") }
        binding.buttonTakePhoto.setOnClickListener { takePhoto() }
        binding.buttonSaveLog.setOnClickListener { saveLog() }

        // Star rating buttons
        listOf(binding.star1, binding.star2, binding.star3, binding.star4, binding.star5)
            .forEachIndexed { idx, tv ->
                tv.setOnClickListener { setRating(idx + 1) }
            }
    }

    private fun setupSpinners() {
        val typeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, TREE_TYPES)
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerTreeType.adapter = typeAdapter

        val seasonAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, SEASONS)
        seasonAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerSeason.adapter = seasonAdapter

        val ripenessAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, RIPENESS_OPTIONS)
        ripenessAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerRipeness.adapter = ripenessAdapter
    }

    private fun populateFields(log: LogEntry) {
        binding.editTextTreeName.setText(log.name)
        binding.editTextLocation.setText(log.location)
        binding.editTextNotes.setText(log.notes)
        currentLat = log.lat
        currentLng = log.lng

        val typeIdx = TREE_TYPES.indexOfFirst { it.startsWith(log.treeType) }
        if (typeIdx >= 0) binding.spinnerTreeType.setSelection(typeIdx)

        val seasonIdx = SEASONS.indexOf(log.season)
        if (seasonIdx >= 0) binding.spinnerSeason.setSelection(seasonIdx)

        val ripenessIdx = RIPENESS_OPTIONS.indexOfFirst { it.contains(log.ripeness) }
        if (ripenessIdx >= 0) binding.spinnerRipeness.setSelection(ripenessIdx)

        setRating(log.rating)

        if (log.lat != null && log.lng != null) {
            binding.tvCoordinates.text = "📍 ${String.format("%.5f", log.lat)}, ${String.format("%.5f", log.lng)}"
            binding.tvCoordinates.visibility = View.VISIBLE
        }
        if (log.imageUri.isNotEmpty()) {
            currentPhotoUri = Uri.parse(log.imageUri)
            loadPhoto(currentPhotoUri!!)
        }
    }

    private var currentRating = 0

    private fun setRating(r: Int) {
        currentRating = r
        val stars = listOf(binding.star1, binding.star2, binding.star3, binding.star4, binding.star5)
        stars.forEachIndexed { idx, tv ->
            tv.text = if (idx < r) "★" else "☆"
        }
    }

    private fun loadPhoto(uri: Uri) {
        currentPhotoUri = uri
        binding.imageViewPreview.visibility = View.VISIBLE
        Glide.with(this).load(uri).centerCrop().into(binding.imageViewPreview)
    }

    private fun getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOC_PERMISSION_CODE
            )
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { loc: Location? ->
            if (loc != null) {
                currentLat = loc.latitude
                currentLng = loc.longitude
                binding.tvCoordinates.text =
                    "📍 ${String.format("%.5f", loc.latitude)}, ${String.format("%.5f", loc.longitude)}"
                binding.tvCoordinates.visibility = View.VISIBLE
                if (binding.editTextLocation.text.isNullOrEmpty()) {
                    binding.editTextLocation.setText(
                        "${String.format("%.5f", loc.latitude)}, ${String.format("%.5f", loc.longitude)}"
                    )
                }
                Toast.makeText(this, "Location captured ✓", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Cannot get location. Try moving outdoors.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun takePhoto() {
        val photoFile = File(getExternalFilesDir(null), "IMG_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(
            this, "${applicationContext.packageName}.fileprovider", photoFile
        )
        currentPhotoUri = uri
        takePictureLauncher.launch(uri)
    }

    private fun saveLog() {
        val treeName = binding.editTextTreeName.text?.toString()?.trim()
        val location = binding.editTextLocation.text?.toString()?.trim()
        val notes = binding.editTextNotes.text?.toString()?.trim() ?: ""

        if (treeName.isNullOrEmpty()) {
            binding.editTextTreeName.error = "Spot name is required"
            binding.editTextTreeName.requestFocus()
            return
        }
        if (location.isNullOrEmpty()) {
            binding.editTextLocation.error = "Location is required"
            binding.editTextLocation.requestFocus()
            return
        }

        val treeTypeRaw = binding.spinnerTreeType.selectedItem?.toString() ?: "Other"
        // Strip emoji for clean DB storage
        val treeType = treeTypeRaw.split(" ").first()
        val season = binding.spinnerSeason.selectedItem?.toString() ?: ""
        val ripenessRaw = binding.spinnerRipeness.selectedItem?.toString() ?: ""
        // Extract clean word from emoji-prefixed option e.g. "🔴 Peak — harvest now!" → "Peak"
        val ripeness = when {
            ripenessRaw.contains("Unripe") -> "Unripe"
            ripenessRaw.contains("Almost") -> "Almost Ready"
            ripenessRaw.contains("Peak") -> "Peak"
            ripenessRaw.contains("Overripe") -> "Overripe"
            else -> ""
        }

        if (currentLat == null || currentLng == null) {
            AlertDialog.Builder(this)
                .setTitle("No GPS coordinates")
                .setMessage("This tree won't appear on the map without GPS.\n\nSave anyway?")
                .setPositiveButton("Save anyway") { _, _ ->
                    doSave(treeName, location, notes, treeType, season, ripeness)
                }
                .setNegativeButton("Add GPS", null)
                .show()
            return
        }
        doSave(treeName, location, notes, treeType, season, ripeness)
    }

    private fun doSave(
        treeName: String, location: String, notes: String,
        treeType: String, season: String, ripeness: String
    ) {
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        val entry = LogEntry(
            id = editingEntry?.id ?: 0,
            name = treeName,
            location = location,
            date = date,
            notes = notes,
            imageUri = currentPhotoUri?.toString() ?: "",
            lat = currentLat,
            lng = currentLng,
            treeType = treeType,
            season = season,
            isFavorite = editingEntry?.isFavorite ?: false,
            ripeness = ripeness,
            rating = currentRating
        )
        if (editingEntry != null) viewModel.update(entry) else viewModel.insert(entry)
        Toast.makeText(this, "🌿 Spot saved!", Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOC_PERMISSION_CODE &&
            grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED
        ) { getCurrentLocation() } else {
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }
}
