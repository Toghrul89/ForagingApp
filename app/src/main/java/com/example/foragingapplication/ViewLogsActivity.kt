package com.example.foragingapplication

import android.content.Context
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ViewLogsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_logs)

        val textViewLogs = findViewById<TextView>(R.id.textViewLogs)
        val sharedPreferences = getSharedPreferences("ForagingLogs", Context.MODE_PRIVATE)

        val allLogs = sharedPreferences.all
        val stringBuilder = StringBuilder()

        for ((_, value) in allLogs) {
            stringBuilder.append(value.toString()).append("\n\n")
        }

        textViewLogs.text = if (allLogs.isNotEmpty()) {
            stringBuilder.toString()
        } else {
            "No logs found."
        }
    }
}

