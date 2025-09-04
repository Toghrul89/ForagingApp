package com.example.foragingapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize buttons from layout
        val logEntryButton = findViewById<Button>(R.id.btnLogEntry)
        val viewLogsButton = findViewById<Button>(R.id.btnViewLogs)
        val openMapButton = findViewById<Button>(R.id.btnOpenMap)
        val viewLogsButton: Button = findViewById(R.id.btnViewLogs)
        viewLogsButton.setOnClickListener {
            val intent = Intent(this, ViewLogsActivity::class.java)
            startActivity(intent)
        }


        // Navigate to LogEntryActivity
        logEntryButton.setOnClickListener {
            val intent = Intent(this, LogEntryActivity::class.java)
            startActivity(intent)
        }

        // Navigate to ViewLogsActivity
        viewLogsButton.setOnClickListener {
            val intent = Intent(this, ViewLogsActivity::class.java)
            startActivity(intent)
        }

        // Navigate to MapActivity
        openMapButton.setOnClickListener {
            val intent = Intent(this, MapActivity::class.java)
            startActivity(intent)
        }
    }
}
