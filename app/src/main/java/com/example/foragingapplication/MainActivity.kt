package com.example.foragingapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val logEntryButton = findViewById<Button>(R.id.btnLogEntry)
        val viewLogsButton = findViewById<Button>(R.id.btnViewLogs)
        val openMapButton = findViewById<Button>(R.id.btnOpenMap)

        logEntryButton.setOnClickListener {
            val intent = Intent(this, LogEntryActivity::class.java)
            startActivity(intent)
        }

        viewLogsButton.setOnClickListener {
            val intent = Intent(this, ViewLogsActivity::class.java)
            startActivity(intent)
        }

        openMapButton.setOnClickListener {
            val intent = Intent(this, MapActivity::class.java)
            startActivity(intent)
        }
    }
}