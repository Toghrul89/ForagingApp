package com.example.foragingapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<CardView>(R.id.btnLogEntry).setOnClickListener {
            startActivity(Intent(this, LogEntryActivity::class.java))
        }

        findViewById<Button>(R.id.btnViewLogs).setOnClickListener {
            startActivity(Intent(this, ViewLogsActivity::class.java))
        }

        findViewById<Button>(R.id.btnOpenMap).setOnClickListener {
            startActivity(Intent(this, MapActivity::class.java))
        }
    }
}