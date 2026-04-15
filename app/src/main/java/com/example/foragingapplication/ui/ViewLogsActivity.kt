package com.example.foragingapp.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.foragingapp.LogViewModel
import com.example.foragingapp.databinding.ActivityViewLogsBinding
import com.google.android.material.snackbar.Snackbar

class ViewLogsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityViewLogsBinding
    private lateinit var adapter: LogsAdapter
    private val viewModel: LogViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewLogsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        adapter = LogsAdapter(
            onEdit = { log ->
                startActivity(Intent(this, LogEntryActivity::class.java).apply {
                    putExtra("LOG_ID", log.id)
                })
            },
            onMapClick = { log ->
                startActivity(Intent(this, MapActivity::class.java).apply {
                    putExtra("FOCUS_LAT", log.lat)
                    putExtra("FOCUS_LNG", log.lng)
                    putExtra("FOCUS_NAME", log.name)
                })
            },
            onFavorite = { log -> viewModel.toggleFavorite(log) }
        )

        binding.rvLogs.layoutManager = LinearLayoutManager(this)
        binding.rvLogs.adapter = adapter

        // Swipe-to-delete
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(
                rv: RecyclerView, vh: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder
            ) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val deleted = adapter.getItemAt(viewHolder.adapterPosition)
                viewModel.delete(deleted)
                Snackbar.make(binding.root, "${deleted.name} removed", Snackbar.LENGTH_LONG)
                    .setAction("Undo") { viewModel.insert(deleted) }
                    .show()
            }
        }).attachToRecyclerView(binding.rvLogs)

        // Search
        binding.searchBar.doAfterTextChanged { text ->
            viewModel.setSearch(text?.toString() ?: "")
        }

        // Default: observe search results
        viewModel.searchResults.observe(this) { logs ->
            if (!binding.chipFavorites.isChecked && !binding.chipPeak.isChecked) {
                updateList(logs)
            }
        }

        // Favorites filter chip
        binding.chipFavorites.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                binding.chipPeak.isChecked = false
                viewModel.allLogs.value?.let { logs ->
                    updateList(logs.filter { it.isFavorite })
                }
            } else {
                viewModel.setSearch(binding.searchBar.text?.toString() ?: "")
            }
        }

        // Peak ripeness filter chip
        binding.chipPeak.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                binding.chipFavorites.isChecked = false
                viewModel.allLogs.value?.let { logs ->
                    updateList(logs.filter { it.ripeness == "Peak" })
                }
            } else {
                viewModel.setSearch(binding.searchBar.text?.toString() ?: "")
            }
        }

        // Observe allLogs to refresh chips
        viewModel.allLogs.observe(this) { logs ->
            if (binding.chipFavorites.isChecked) updateList(logs.filter { it.isFavorite })
            else if (binding.chipPeak.isChecked) updateList(logs.filter { it.ripeness == "Peak" })
        }
    }

    private fun updateList(logs: List<com.example.foragingapp.model.LogEntry>) {
        adapter.submitList(logs)
        binding.tvEmpty.visibility = if (logs.isEmpty()) View.VISIBLE else View.GONE
        binding.rvLogs.visibility = if (logs.isEmpty()) View.GONE else View.VISIBLE
    }
}
