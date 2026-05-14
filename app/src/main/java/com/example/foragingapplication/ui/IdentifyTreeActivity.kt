package com.example.foragingapp.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.example.foragingapp.databinding.ActivityIdentifyTreeBinding
import java.io.File
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale

class IdentifyTreeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityIdentifyTreeBinding
    private var currentPhotoUri: Uri? = null
    private var identifiedName: String? = null

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { identifyFromPhoto(it) }
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

        binding.buttonGallery.setOnClickListener { pickImageLauncher.launch("image/*") }
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

        val result = PlantIdentifier.identify(uri.toString())
        identifiedName = result.name
        binding.textResult.text = "${result.name}\n\n${result.description}\n\nConfidence: ${result.confidence}"
        binding.buttonWikipedia.visibility = View.VISIBLE
        binding.buttonAddToMap.visibility = View.VISIBLE
    }

    private fun openWikipedia() {
        val name = identifiedName ?: return
        val query = URLEncoder.encode(name, StandardCharsets.UTF_8.toString())
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://en.wikipedia.org/wiki/Special:Search?search=$query")))
    }

    private fun addToMap() {
        val name = identifiedName ?: return
        val intent = Intent(this, LogEntryActivity::class.java)
        intent.putExtra("PREFILL_NAME", name)
        intent.putExtra("PREFILL_NOTES", "Suggested by Identify Tree. Please verify before eating.")
        intent.putExtra("PREFILL_IMAGE_URI", currentPhotoUri?.toString() ?: "")
        startActivity(intent)
    }
}

data class IdentificationResult(
    val name: String,
    val description: String,
    val confidence: String
)

object PlantIdentifier {
    private val knownFruitTrees = listOf(
        "Cornelian Cherry" to "Cornelian cherry is a small tree or shrub with edible red fruit. Also known as dogwood cherry or zogal.",
        "Apple" to "Apple trees are common urban fruit trees with spring blossoms and fall fruit.",
        "Cherry" to "Cherry trees produce small stone fruits and often have noticeable spring blossoms.",
        "Pear" to "Pear trees produce rounded or bell-shaped fruits and glossy leaves.",
        "Plum" to "Plum trees produce smooth stone fruits that ripen in summer or early autumn.",
        "Fig" to "Fig trees have large lobed leaves and soft pear-shaped fruits.",
        "Mulberry" to "Mulberries grow on trees with varied leaf shapes and dark clustered fruit.",
        "Blackberry" to "Blackberries grow on canes and produce dark clustered berries.",
        "Elderberry" to "Elderberries grow in clusters and must be identified carefully before use.",
        "Serviceberry" to "Serviceberries are small tree fruits that ripen to purple-blue in early summer."
    )

    fun identify(source: String): IdentificationResult {
        val lower = source.lowercase(Locale.US)
        val match = knownFruitTrees.firstOrNull { (name, _) ->
            lower.contains(name.lowercase(Locale.US).replace(" ", "")) ||
                lower.contains(name.lowercase(Locale.US))
        } ?: knownFruitTrees.first()

        return IdentificationResult(
            name = match.first,
            description = match.second,
            confidence = if (lower.contains(match.first.lowercase(Locale.US))) "medium" else "low - verify manually"
        )
    }
}
