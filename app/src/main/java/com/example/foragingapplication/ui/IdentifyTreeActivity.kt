package com.example.foragingapp.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.example.foragingapp.auth.AuthManager
import com.example.foragingapp.databinding.ActivityIdentifyTreeBinding
import kotlinx.coroutines.launch
import java.io.File
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale

class IdentifyTreeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityIdentifyTreeBinding
    private var currentPhotoUri: Uri? = null
    private var identifiedResult: IdentificationResult? = null

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            identifyFromPhoto(it)
        }
    }

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) currentPhotoUri?.let { identifyFromPhoto(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIdentifyTreeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.buttonGallery.setOnClickListener { pickImageLauncher.launch(arrayOf("image/*")) }
        binding.buttonCamera.setOnClickListener { takePhoto() }
        binding.buttonWikipedia.setOnClickListener { openWikipedia() }
        binding.buttonAddToMap.setOnClickListener { addToMap() }
    }

    private fun takePhoto() {
        val photoFile = File(getExternalFilesDir(null), "IDENTIFY_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(this, "${applicationContext.packageName}.fileprovider", photoFile)
        currentPhotoUri = uri
        takePictureLauncher.launch(uri)
    }

    private fun identifyFromPhoto(uri: Uri) {
        currentPhotoUri = uri
        binding.imagePreview.visibility = View.VISIBLE
        Glide.with(this).load(uri).centerCrop().into(binding.imagePreview)

        binding.textResult.text = "Checking this photo with PlantNet..."
        binding.buttonWikipedia.visibility = View.GONE
        binding.buttonAddToMap.visibility = View.GONE

        lifecycleScope.launch {
            val result = PlantNetClient.identify(this@IdentifyTreeActivity, uri)
                ?: PlantIdentifier.identify(uri.toString())
            showResult(result)
        }
    }

    private fun showResult(result: IdentificationResult) {
        identifiedResult = result
        binding.textResult.text =
            "${result.name}\n${result.scientificName}\n\n${result.description}\n\nSeason: ${result.season}\nCategory: ${result.category}\nFruit match confidence: ${result.confidence}\n\nThis is not a safety confirmation. Verify before eating."
        binding.buttonWikipedia.visibility = View.VISIBLE
        binding.buttonAddToMap.visibility = View.VISIBLE
    }

    private fun openWikipedia() {
        val result = identifiedResult ?: return
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(result.wikipediaUrl.ifBlank {
            val query = URLEncoder.encode(result.name, StandardCharsets.UTF_8.toString())
            "https://en.wikipedia.org/wiki/Special:Search?search=$query"
        })))
    }

    private fun addToMap() {
        val result = identifiedResult ?: return
        if (!AuthManager.isSignedIn(this)) {
            android.widget.Toast.makeText(this, "Please sign in to contribute discoveries.", android.widget.Toast.LENGTH_LONG).show()
            startActivity(Intent(this, AuthActivity::class.java))
            return
        }
        val intent = Intent(this, LogEntryActivity::class.java)
        intent.putExtra("PREFILL_NAME", result.name)
        intent.putExtra("PREFILL_NOTES", "Suggested fruit match from Identify Tree. Please verify before eating.")
        intent.putExtra("PREFILL_IMAGE_URI", currentPhotoUri?.toString() ?: "")
        intent.putExtra("PREFILL_SCIENTIFIC_NAME", result.scientificName)
        intent.putExtra("PREFILL_SEASON", result.season)
        intent.putExtra("PREFILL_CATEGORY", result.category)
        intent.putExtra("PREFILL_WIKIPEDIA_URL", result.wikipediaUrl)
        startActivity(intent)
    }
}

data class IdentificationResult(
    val name: String,
    val description: String,
    val confidence: String,
    val scientificName: String = "",
    val season: String = "Seasonal",
    val category: String = "Fruit Tree",
    val wikipediaUrl: String = ""
)

object PlantIdentifier {
    private val knownFruitTrees = listOf(
        FruitHint("Cornelian Cherry", "Cornus mas", "Summer", "Stone Fruit", "Cornelian cherry is a fruiting dogwood with edible red fruit. Also known as dogwood cherry or zogal.", "https://en.wikipedia.org/wiki/Cornelian_cherry"),
        FruitHint("Apple", "Malus domestica", "Autumn", "Fruit Tree", "Apple trees are common urban fruit trees with spring blossoms and fall fruit.", "https://en.wikipedia.org/wiki/Apple"),
        FruitHint("Cherry", "Prunus", "Summer", "Stone Fruit", "Cherry trees produce small stone fruits and often have noticeable spring blossoms.", "https://en.wikipedia.org/wiki/Cherry"),
        FruitHint("Pear", "Pyrus", "Autumn", "Fruit Tree", "Pear trees produce rounded or bell-shaped fruits and glossy leaves.", "https://en.wikipedia.org/wiki/Pear"),
        FruitHint("Plum", "Prunus domestica", "Summer", "Stone Fruit", "Plum trees produce smooth stone fruits that ripen in summer or early autumn.", "https://en.wikipedia.org/wiki/Plum"),
        FruitHint("Fig", "Ficus carica", "Summer", "Fruit Tree", "Fig trees have large lobed leaves and soft pear-shaped fruits.", "https://en.wikipedia.org/wiki/Fig"),
        FruitHint("Mulberry", "Morus", "Summer", "Berry", "Mulberries grow on trees with varied leaf shapes and dark clustered fruit.", "https://en.wikipedia.org/wiki/Morus_(plant)"),
        FruitHint("Blackberry", "Rubus", "Summer", "Berry", "Blackberries are fruiting canes with dark clustered berries.", "https://en.wikipedia.org/wiki/Blackberry"),
        FruitHint("Elderberry", "Sambucus", "Summer", "Berry", "Elderberries grow in fruit clusters and must be identified carefully before use.", "https://en.wikipedia.org/wiki/Sambucus"),
        FruitHint("Serviceberry", "Amelanchier", "Summer", "Berry", "Serviceberries are small tree fruits that ripen to purple-blue in early summer.", "https://en.wikipedia.org/wiki/Amelanchier")
    )

    fun identify(source: String): IdentificationResult {
        val lower = source.lowercase(Locale.US)
        val match = knownFruitTrees.firstOrNull { hint ->
            lower.contains(hint.name.lowercase(Locale.US).replace(" ", "")) ||
                lower.contains(hint.name.lowercase(Locale.US))
        } ?: knownFruitTrees.first()

        return IdentificationResult(
            name = match.name,
            description = match.description,
            confidence = if (lower.contains(match.name.lowercase(Locale.US))) "medium" else "low - verify manually",
            scientificName = match.scientificName,
            season = match.season,
            category = match.category,
            wikipediaUrl = match.wikipediaUrl
        )
    }

    private data class FruitHint(
        val name: String,
        val scientificName: String,
        val season: String,
        val category: String,
        val description: String,
        val wikipediaUrl: String
    )
}
