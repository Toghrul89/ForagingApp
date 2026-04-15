package com.example.foragingapp.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.foragingapp.LogViewModel
import com.example.foragingapp.R
import com.example.foragingapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: LogViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Update spot counter badge on the home card
        viewModel.allLogs.observe(this) { logs ->
            val count = logs.size
            binding.tvSpotCount.text = if (count == 1) "1 tree spotted" else "$count trees spotted"
            binding.tvFavCount.text = "${logs.count { it.isFavorite }} favourites"
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
    }
}