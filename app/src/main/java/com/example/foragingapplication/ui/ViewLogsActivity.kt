package com.example.foragingapp.ui

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
                val intent = android.content.Intent(this, LogEntryActivity::class.java)
                intent.putExtra("LOG_ID", log.id)
                startActivity(intent)
            },
            onMapClick = { log ->
                val intent = android.content.Intent(this, MapActivity::class.java)
                intent.putExtra("FOCUS_LAT", log.lat)
                intent.putExtra("FOCUS_LNG", log.lng)
                intent.putExtra("FOCUS_NAME", log.name)
                startActivity(intent)
            },
            onFavorite = { log -> viewModel.toggleFavorite(log) }
        )

        binding.rvLogs.layoutManager = LinearLayoutManager(this)
        binding.rvLogs.adapter = adapter

        // Swipe-to-delete
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = false
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val pos = viewHolder.adapterPosition
                val deleted = adapter.getItemAt(pos)
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

        viewModel.searchResults.observe(this) { logs ->
            adapter.submitList(logs)
            binding.tvEmpty.visibility = if (logs.isEmpty()) View.VISIBLE else View.GONE
            binding.rvLogs.visibility = if (logs.isEmpty()) View.GONE else View.VISIBLE
        }
    }
}