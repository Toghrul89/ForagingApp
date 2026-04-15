package com.example.foragingapp.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
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
            binding.tvFavCount.text = "${logs.count { it.isFavorite }} favourites"

            // Show seasonal tip based on most recent log's season
            val seasonalTip = when (java.util.Calendar.getInstance().get(java.util.Calendar.MONTH)) {
                in 2..4 -> "🌸 Spring: look for cherry blossoms turning to fruit!"
                in 5..7 -> "☀️ Summer: peak season for mulberries & cornelian cherries!"
                in 8..10 -> "🍂 Autumn: apple, pear and quince time!"
                else -> "❄️ Winter: plan your spots for next season."
            }
            binding.tvSeasonalTip.text = seasonalTip
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
        binding.cardAbout.setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }
    }
}
