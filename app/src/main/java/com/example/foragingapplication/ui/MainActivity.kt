package com.example.foragingapp.ui

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import com.example.foragingapp.LogViewModel
import com.example.foragingapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: LogViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
            startActivity(Intent(this, LogEntryActivity::class.java))
        }

        binding.cardViewLogs.setOnClickListener {
            startActivity(Intent(this, ViewLogsActivity::class.java))
        }

        binding.cardOpenMap.setOnClickListener {
            startActivity(Intent(this, MapActivity::class.java))
        }

        binding.cardSavedHome.setOnClickListener {
            startActivity(Intent(this, ViewLogsActivity::class.java))
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
            startActivity(Intent(this, AboutActivity::class.java))
        }
    }

    private fun openLogsSearch(query: String) {
        val intent = Intent(this, ViewLogsActivity::class.java)
        if (query.isNotBlank()) intent.putExtra("SEARCH_QUERY", query)
        startActivity(intent)
    }
}
