package com.example.foragingapp.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.widget.doAfterTextChanged
import com.example.foragingapp.LogViewModel
import com.example.foragingapp.auth.AuthManager
import com.example.foragingapp.R
import com.example.foragingapp.databinding.ActivityMainBinding
import com.example.foragingapp.model.LogEntry

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
            renderHomeDiscoverySections(logs)
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

    private fun renderHomeDiscoverySections(logs: List<LogEntry>) {
        renderLogCards(
            binding.recentlyAddedContainer,
            logs.sortedByDescending { it.createdAt.ifBlank { it.date } }.take(3),
            "Official Seattle fruit data will appear here when the map syncs."
        )
        renderLogCards(
            binding.communityHighlightsContainer,
            logs.filter { it.dataSource != "OFFICIAL" }.take(3),
            "Community discoveries will appear here as neighbors add fruit trees."
        )
    }

    private fun renderLogCards(container: LinearLayout, logs: List<LogEntry>, emptyText: String) {
        container.removeAllViews()
        if (logs.isEmpty()) {
            val empty = TextView(this)
            empty.text = emptyText
            empty.setTextColor(getColor(R.color.textSecondary))
            empty.setPadding(18, 18, 18, 18)
            container.addView(empty)
            return
        }
        logs.forEach { log -> container.addView(homeLogCard(log)) }
    }

    private fun homeLogCard(log: LogEntry): View {
        val card = CardView(this)
        card.radius = 18f
        card.cardElevation = 0f
        card.setCardBackgroundColor(getColor(R.color.surface))
        card.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = 10 }

        val column = LinearLayout(this)
        column.orientation = LinearLayout.VERTICAL
        column.setPadding(18, 16, 18, 16)

        val title = TextView(this)
        title.text = log.name
        title.setTextColor(getColor(R.color.textPrimary))
        title.textSize = 16f
        title.setTypeface(title.typeface, android.graphics.Typeface.BOLD)

        val subtitle = TextView(this)
        subtitle.text = buildString {
            append(if (log.dataSource == "OFFICIAL") "Official Dataset" else "Community Contribution")
            if (log.season.isNotBlank()) append(" • ${log.season}")
            if (log.accessType.isNotBlank()) append(" • ${log.accessType}")
        }
        subtitle.setTextColor(getColor(R.color.textSecondary))
        subtitle.textSize = 13f
        subtitle.setPadding(0, 5, 0, 0)

        val source = TextView(this)
        source.text = log.sourceLabel.ifBlank { log.location }
        source.setTextColor(getColor(R.color.textHint))
        source.textSize = 12f
        source.setPadding(0, 5, 0, 0)

        column.addView(title)
        column.addView(subtitle)
        column.addView(source)
        card.addView(column)
        card.setOnClickListener {
            startActivity(Intent(this, TreeDetailsActivity::class.java).putExtra("LOG_ID", log.id))
        }
        return card
    }
}
