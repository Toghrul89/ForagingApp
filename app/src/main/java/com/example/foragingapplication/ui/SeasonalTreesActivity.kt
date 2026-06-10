package com.example.foragingapp.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.foragingapp.LogViewModel
import com.example.foragingapp.R
import com.example.foragingapp.auth.AuthManager
import com.example.foragingapp.databinding.ActivitySeasonalTreesBinding
import com.example.foragingapp.model.LogEntry
import java.util.Locale

class SeasonalTreesActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySeasonalTreesBinding
    private lateinit var adapter: LogsAdapter
    private val viewModel: LogViewModel by viewModels()
    private var allLogs: List<LogEntry> = emptyList()

    private val seasonalFruit = listOf(
        SeasonalFruit("Blackberry", "Rubus fruticosus", "Common late-summer fruit along sunny edges and trails."),
        SeasonalFruit("Elderberry", "Sambucus nigra", "Clusters of dark berries; verify carefully before any use."),
        SeasonalFruit("Wild Raspberry", "Rubus idaeus", "Soft red berries on canes, often found near open green spaces."),
        SeasonalFruit("Serviceberry", "Amelanchier", "Small purple-blue fruits that ripen around early summer."),
        SeasonalFruit("Cornelian Cherry", "Cornus mas", "Red dogwood cherry fruit, also known as zogal."),
        SeasonalFruit("Crabapple", "Malus", "Small urban apples that often ripen in late summer or fall.")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySeasonalTreesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        adapter = LogsAdapter(
            onEdit = { log ->
                startActivity(Intent(this, TreeDetailsActivity::class.java).putExtra("LOG_ID", log.id))
            },
            onMapClick = { log ->
                startActivity(
                    Intent(this, MapActivity::class.java)
                        .putExtra("FOCUS_LAT", log.lat)
                        .putExtra("FOCUS_LNG", log.lng)
                        .putExtra("FOCUS_NAME", log.name)
                )
            },
            onFavorite = { log ->
                if (AuthManager.isSignedIn(this)) viewModel.toggleFavorite(log)
                else {
                    Toast.makeText(this, "Please sign in to save trees.", Toast.LENGTH_LONG).show()
                    startActivity(Intent(this, AuthActivity::class.java))
                }
            }
        )
        binding.rvLogs.layoutManager = LinearLayoutManager(this)
        binding.rvLogs.adapter = adapter

        binding.searchBar.doAfterTextChanged { render(it?.toString().orEmpty()) }
        viewModel.allLogs.observe(this) { logs ->
            allLogs = logs
            render(binding.searchBar.text?.toString().orEmpty())
        }
    }

    private fun render(query: String) {
        val normalizedQuery = query.trim().lowercase(Locale.US)
        renderStaticSeasonalFruit(normalizedQuery)

        val seasonalNames = seasonalFruit.map { it.name.lowercase(Locale.US) }
        val communitySeasonal = allLogs.filter { log ->
            val haystack = "${log.name} ${log.treeType} ${log.season} ${log.location}".lowercase(Locale.US)
            val matchesSeasonalFruit = seasonalNames.any { haystack.contains(it.lowercase(Locale.US)) } ||
                log.season.equals("Summer", ignoreCase = true) ||
                log.season.equals("Autumn", ignoreCase = true)
            val matchesSearch = normalizedQuery.isBlank() || haystack.contains(normalizedQuery)
            matchesSeasonalFruit && matchesSearch
        }

        adapter.submitList(communitySeasonal)
        binding.tvEmpty.visibility = if (communitySeasonal.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun renderStaticSeasonalFruit(query: String) {
        binding.staticSeasonContainer.removeAllViews()
        seasonalFruit
            .filter { fruit ->
                query.isBlank() ||
                    fruit.name.lowercase(Locale.US).contains(query) ||
                    fruit.scientific.lowercase(Locale.US).contains(query) ||
                    fruit.note.lowercase(Locale.US).contains(query)
            }
            .forEach { fruit ->
                binding.staticSeasonContainer.addView(seasonalFruitCard(fruit))
            }
    }

    private fun seasonalFruitCard(fruit: SeasonalFruit): View {
        val card = CardView(this)
        card.radius = 20f
        card.cardElevation = 0f
        card.setCardBackgroundColor(getColor(R.color.surface))
        card.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = 10 }

        val column = LinearLayout(this)
        column.orientation = LinearLayout.VERTICAL
        column.setPadding(18, 16, 18, 16)

        val name = TextView(this)
        name.text = fruit.name
        name.setTextColor(getColor(R.color.textPrimary))
        name.textSize = 17f
        name.setTypeface(name.typeface, android.graphics.Typeface.BOLD)

        val scientific = TextView(this)
        scientific.text = fruit.scientific
        scientific.setTextColor(getColor(R.color.textHint))
        scientific.textSize = 13f

        val note = TextView(this)
        note.text = fruit.note
        note.setTextColor(getColor(R.color.textSecondary))
        note.textSize = 14f
        note.setPadding(0, 8, 0, 0)

        column.addView(name)
        column.addView(scientific)
        column.addView(note)
        card.addView(column)
        return card
    }

    private data class SeasonalFruit(
        val name: String,
        val scientific: String,
        val note: String
    )
}
