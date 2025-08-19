package com.example.foragingapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.buttonAddLog).setOnClickListener {
            startActivity(Intent(this, LogEntryActivity::class.java))
        }

        findViewById<Button>(R.id.buttonViewLogs).setOnClickListener {
            startActivity(Intent(this, LogListActivity::class.java))
        }

        findViewById<Button>(R.id.buttonMap).setOnClickListener {
            startActivity(Intent(this, MapActivity::class.java))
        }
    }
}