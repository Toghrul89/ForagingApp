package com.example.foragingapp.ui

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import com.example.foragingapp.LogViewModel
import com.example.foragingapp.auth.AuthManager
import com.example.foragingapp.R
import com.example.foragingapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: LogViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        showSafetyDisclaimerIfNeeded()
        viewModel.deleteOldIncorrectCornelianCherry()
        viewModel.importOfficialSeattleTrees()

        viewModel.allLogs.observe(this) { logs ->
            val count = logs.size
            binding.tvSpotCount.text = if (count == 1) "1 tree spotted" else "$count trees spotted"
            val savedCount = logs.count { it.isFavorite }
            binding.tvFavCount.text = if (savedCount == 1) "1 saved" else "$savedCount saved"

            val suggestions = logs.flatMap { listOf(it.name, it.treeType, it.location) }
                .filter { it.isNotBlank() }
                .distinct()
                .sorted()
            binding.homeSearchInput.setAdapter(
                ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, suggestions)
            )
        }

        binding.cardSearchHome.setOnClickListener {
            openLogsSearch(binding.homeSearchInput.text?.toString().orEmpty())
        }

        binding.homeSearchInput.doAfterTextChanged { text ->
            if (!text.isNullOrBlank()) binding.homeSearchInput.showDropDown()
        }

        binding.homeSearchInput.setOnEditorActionListener { _, _, _ ->
            openLogsSearch(binding.homeSearchInput.text?.toString().orEmpty())
            true
        }

        binding.homeSearchInput.setOnItemClickListener { _, _, _, _ ->
            openLogsSearch(binding.homeSearchInput.text?.toString().orEmpty())
        }

        binding.buttonLocateHome.setOnClickListener {
            startActivity(Intent(this, MapActivity::class.java))
        }

        binding.cardLogEntry.setOnClickListener {
            requireSignInForContribution()
        }

        binding.cardViewLogs.setOnClickListener {
            startActivity(Intent(this, ViewLogsActivity::class.java))
        }

        binding.cardOpenMap.setOnClickListener {
            startActivity(Intent(this, MapActivity::class.java))
        }

        binding.cardSavedHome.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        binding.cardSeasonMore.root.setOnClickListener {
            startActivity(Intent(this, SeasonalTreesActivity::class.java))
        }

        binding.navExplore.setOnClickListener {
            startActivity(Intent(this, ViewLogsActivity::class.java))
        }

        binding.navIdentify.setOnClickListener {
            startActivity(Intent(this, IdentifyTreeActivity::class.java))
        }

        binding.navMap.setOnClickListener {
            startActivity(Intent(this, MapActivity::class.java))
        }

        binding.navProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }

    private fun requireSignInForContribution() {
        if (AuthManager.isSignedIn(this)) {
            startActivity(Intent(this, LogEntryActivity::class.java))
        } else {
            Toast.makeText(this, "Please sign in to contribute discoveries.", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, AuthActivity::class.java))
        }
    }

    private fun openLogsSearch(query: String) {
        val intent = Intent(this, ViewLogsActivity::class.java)
        if (query.isNotBlank()) intent.putExtra("SEARCH_QUERY", query)
        startActivity(intent)
    }

    private fun showSafetyDisclaimerIfNeeded() {
        if (DisclaimerManager.hasAccepted(this)) return
        AlertDialog.Builder(this)
            .setTitle("Before you explore")
            .setMessage(
                "${getString(R.string.safety_disclaimer)}\n\n" +
                    "Respect private property, sidewalks, parks, wildlife, and neighbors. ZOGAL is an urban discovery map, not an edible safety authority."
            )
            .setPositiveButton("I Understand") { _, _ -> DisclaimerManager.accept(this) }
            .setCancelable(false)
            .show()
    }
}
