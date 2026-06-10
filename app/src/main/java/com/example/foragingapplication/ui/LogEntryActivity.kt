package com.example.foragingapp.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.foragingapp.LogViewModel
import com.example.foragingapp.auth.AuthManager
import com.example.foragingapp.databinding.ActivityLogEntryBinding
import com.example.foragingapp.model.LogEntry
import kotlinx.coroutines.launch
import java.io.File
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*

class LogEntryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLogEntryBinding
    private val viewModel: LogViewModel by viewModels()

    private var currentPhotoUri: Uri? = null
    private var currentLat: Double? = null
    private var currentLng: Double? = null
    private var editingEntry: LogEntry? = null
    private var suggestedScientificName: String = ""
    private var suggestedFruitCategory: String = ""

    companion object {
        val TREE_TYPES = listOf(
            "Apple", "Cherry", "Pear", "Plum",
            "Mulberry", "Fig", "Blackberry", "Elderberry",
            "Quince", "Persimmon", "Crabapple", "Hawthorn",
            "Serviceberry", "Cornelian Cherry", "Other Fruit"
        )

        val SEASONS = listOf("Spring", "Summer", "Autumn", "Winter", "Year-round")
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            loadPhoto(it)
        }
    }

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) currentPhotoUri?.let { loadPhoto(it) }
    }

    private val locationPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK) return@registerForActivityResult
        val data = result.data ?: return@registerForActivityResult
        val lat = data.getDoubleExtra(LocationPickerActivity.EXTRA_LAT, Double.MIN_VALUE)
        val lng = data.getDoubleExtra(LocationPickerActivity.EXTRA_LNG, Double.MIN_VALUE)
        if (lat == Double.MIN_VALUE || lng == Double.MIN_VALUE) return@registerForActivityResult
        currentLat = lat
        currentLng = lng
        val label = data.getStringExtra(LocationPickerActivity.EXTRA_LOCATION_LABEL)
            ?: "Lat: ${String.format(Locale.US, "%.5f", lat)}, Lng: ${String.format(Locale.US, "%.5f", lng)}"
        binding.editTextLocation.setText(label)
        binding.tvCoordinates.text = "Selected: ${String.format(Locale.US, "%.5f", lat)}, ${String.format(Locale.US, "%.5f", lng)}"
        binding.tvCoordinates.visibility = View.VISIBLE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!AuthManager.isSignedIn(this)) {
            Toast.makeText(this, "Please sign in to contribute discoveries.", Toast.LENGTH_LONG).show()
            startActivity(android.content.Intent(this, AuthActivity::class.java))
            finish()
            return
        }
        binding = ActivityLogEntryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        setupSpinners()

        // Edit mode
        val editingId = intent.getLongExtra("LOG_ID", -1).takeIf { it != -1L }
        if (editingId != null) {
            lifecycleScope.launch {
                viewModel.getLogById(editingId)?.let { log ->
                    val user = AuthManager.currentUser(this@LogEntryActivity)
                    if (user == null || log.creatorUserId != user.id) {
                        Toast.makeText(this@LogEntryActivity, "You can edit only trees you added.", Toast.LENGTH_LONG).show()
                        finish()
                        return@launch
                    }
                    editingEntry = log
                    populateFields(log)
                    supportActionBar?.title = "Edit Tree"
                    binding.buttonSaveLog.text = "Update Tree"
                }
            }
        } else {
            supportActionBar?.title = "New Spot"
            applyPrefill()
        }

        binding.buttonGetLocation.setOnClickListener { openLocationPicker() }
        binding.buttonSelectImage.setOnClickListener { pickImageLauncher.launch(arrayOf("image/*")) }
        binding.buttonTakePhoto.setOnClickListener { takePhoto() }
        binding.buttonSaveLog.setOnClickListener { saveLog() }
    }

    private fun setupSpinners() {
        val typeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, TREE_TYPES)
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerTreeType.adapter = typeAdapter

        val seasonAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, SEASONS)
        seasonAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerSeason.adapter = seasonAdapter
    }

    private fun applyPrefill() {
        intent.getStringExtra("PREFILL_NAME")?.let { binding.editTextTreeName.setText(it) }
        intent.getStringExtra("PREFILL_NOTES")?.let { binding.editTextNotes.setText(it) }
        suggestedScientificName = intent.getStringExtra("PREFILL_SCIENTIFIC_NAME").orEmpty()
        suggestedFruitCategory = intent.getStringExtra("PREFILL_CATEGORY").orEmpty()
        intent.getStringExtra("PREFILL_WIKIPEDIA_URL")?.let { binding.editTextWikipedia.setText(it) }
        intent.getStringExtra("PREFILL_SEASON")?.let { season ->
            val index = SEASONS.indexOfFirst { it.equals(season, ignoreCase = true) }
            if (index >= 0) binding.spinnerSeason.setSelection(index)
        }
        intent.getStringExtra("PREFILL_IMAGE_URI")?.takeIf { it.isNotBlank() }?.let { uriString ->
            loadPhoto(Uri.parse(uriString))
        }
    }

    private fun populateFields(log: LogEntry) {
        binding.editTextTreeName.setText(log.name)
        binding.editTextLocation.setText(log.location)
        binding.editTextNotes.setText(log.notes)
        binding.editTextWikipedia.setText(log.wikipediaUrl)
        binding.checkPublic.isChecked = log.isPublic
        binding.checkVerified.isChecked = log.verificationStatus != "Needs verification"
        currentLat = log.lat
        currentLng = log.lng

        val typeIdx = TREE_TYPES.indexOfFirst { it.startsWith(log.treeType) }
        if (typeIdx >= 0) binding.spinnerTreeType.setSelection(typeIdx)

        val seasonIdx = SEASONS.indexOf(log.season)
        if (seasonIdx >= 0) binding.spinnerSeason.setSelection(seasonIdx)

        if (log.lat != null && log.lng != null) {
            binding.tvCoordinates.text = "📍 ${String.format("%.5f", log.lat)}, ${String.format("%.5f", log.lng)}"
            binding.tvCoordinates.visibility = View.VISIBLE
        }
        if (log.imageUri.isNotEmpty()) {
            currentPhotoUri = Uri.parse(log.imageUri)
            loadPhoto(currentPhotoUri!!)
        }
    }

    private fun loadPhoto(uri: Uri) {
        currentPhotoUri = uri
        binding.imageViewPreview.visibility = View.VISIBLE
        Glide.with(this).load(uri).centerCrop().into(binding.imageViewPreview)
    }

    private fun openLocationPicker() {
        val intent = Intent(this, LocationPickerActivity::class.java)
        currentLat?.let { intent.putExtra(LocationPickerActivity.EXTRA_LAT, it) }
        currentLng?.let { intent.putExtra(LocationPickerActivity.EXTRA_LNG, it) }
        locationPickerLauncher.launch(intent)
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
        val manualWikipediaUrl = binding.editTextWikipedia.text?.toString()?.trim() ?: ""

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
        val treeType = treeTypeRaw
        val season = binding.spinnerSeason.selectedItem?.toString() ?: ""
        val isPublic = binding.checkPublic.isChecked
        val verificationStatus = if (binding.checkVerified.isChecked) "User verified" else "Needs verification"

        if (currentLat == null || currentLng == null) {
            AlertDialog.Builder(this)
                .setTitle("No GPS coordinates")
                .setMessage("This tree won't appear on the map without GPS.\n\nSave anyway?")
                .setPositiveButton("Save anyway") { _, _ -> doSave(treeName, location, notes, treeType, season, manualWikipediaUrl, isPublic, verificationStatus) }
                .setNegativeButton("Add GPS", null)
                .show()
            return
        }
        doSave(treeName, location, notes, treeType, season, manualWikipediaUrl, isPublic, verificationStatus)
    }

    private fun doSave(
        treeName: String, location: String, notes: String,
        treeType: String, season: String, wikipediaUrl: String,
        isPublic: Boolean, verificationStatus: String
    ) {
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        val user = AuthManager.currentUser(this)
        val generatedWikipediaUrl = wikipediaUrl.takeIf { it.startsWith("http://") || it.startsWith("https://") }
            ?: generateWikipediaUrl(treeName, treeType)
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
            wikipediaUrl = generatedWikipediaUrl,
            creatorUserId = editingEntry?.creatorUserId ?: user?.id.orEmpty(),
            creatorName = editingEntry?.creatorName ?: user?.fullName.orEmpty(),
            createdAt = editingEntry?.createdAt?.ifBlank { date } ?: date,
            isPublic = isPublic,
            verificationStatus = verificationStatus,
            isReported = editingEntry?.isReported ?: false,
            dataSource = editingEntry?.dataSource ?: "COMMUNITY",
            scientificName = editingEntry?.scientificName?.ifBlank { suggestedScientificName } ?: suggestedScientificName,
            accessType = if (isPublic) "Public" else "Private",
            fruitCategory = suggestedFruitCategory.ifBlank { fruitCategoryFor(treeType) },
            sourceLabel = editingEntry?.sourceLabel ?: user?.fullName?.takeIf { it.isNotBlank() }?.let { "Added by $it" }.orEmpty()
        )
        if (editingEntry != null) viewModel.update(entry) else viewModel.insert(entry)
        Toast.makeText(this, "Discovery saved", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun generateWikipediaUrl(treeName: String, treeType: String): String {
        val sourceName = treeName.takeIf { it.isNotBlank() } ?: treeType
        val cleaned = sourceName
            .replace(Regex("\\b(by|near|at|tree|spot)\\b.*", RegexOption.IGNORE_CASE), "")
            .trim()
            .ifBlank { treeType }
        val canonicalTitle = when (cleaned.lowercase(Locale.US)) {
            "cornelian cherry", "cornelian", "dogwood cherry", "zogal", "zoğal" -> "Cornelian_cherry"
            "apple", "crabapple", "crab apple" -> if (cleaned.contains("crab", ignoreCase = true)) "Crabapple" else "Apple"
            "blackberry", "wild blackberry" -> "Blackberry"
            "elderberry" -> "Sambucus"
            "wild raspberry", "raspberry" -> "Raspberry"
            "serviceberry" -> "Amelanchier"
            "mulberry" -> "Morus_(plant)"
            "fig" -> "Fig"
            "pear" -> "Pear"
            "plum" -> "Plum"
            "cherry" -> "Cherry"
            "quince" -> "Quince"
            "persimmon" -> "Persimmon"
            "hawthorn" -> "Crataegus"
            else -> URLEncoder.encode(cleaned, StandardCharsets.UTF_8.toString()).replace("+", "_")
        }
        return "https://en.wikipedia.org/wiki/$canonicalTitle"
    }

    private fun fruitCategoryFor(treeType: String): String {
        val lower = treeType.lowercase(Locale.US)
        return when {
            "berry" in lower -> "Berry"
            "cherry" in lower || "plum" in lower -> "Stone Fruit"
            "hazelnut" in lower -> "Nut"
            else -> "Fruit Tree"
        }
    }
}
