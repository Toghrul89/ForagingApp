package com.example.foragingapplication

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Connect the button
        val buttonAddLog = findViewById<Button>(R.id.buttonAddLog)

        // Set click listener
        buttonAddLog.setOnClickListener {
            val intent = Intent(this, LogEntryActivity::class.java)
            startActivity(intent)
        }
    }
}