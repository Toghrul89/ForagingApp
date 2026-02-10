package com.example.foragingapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.foragingapp.data.LogDatabaseHelper
import com.example.foragingapp.model.LogEntry
import java.text.SimpleDateFormat
import java.util.*

class LogEntryActivity : AppCompatActivity() {

    private lateinit var dbHelper: LogDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log_entry)

        dbHelper = LogDatabaseHelper(this)

        val editTextTreeName = findViewById<EditText>(R.id.editTextTreeName)
        val editTextLocation = findViewById<EditText>(R.id.editTextLocation)
        val editTextNotes = findViewById<EditText>(R.id.editTextNotes)
        val buttonSave = findViewById<Button>(R.id.buttonSaveLog)

        buttonSave.setOnClickListener {
            val treeName = editTextTreeName.text?.toString()?.trim()
            val location = editTextLocation.text?.toString()?.trim()
            val notes = editTextNotes.text?.toString()?.trim() ?: ""

            if (treeName.isNullOrEmpty() || location.isNullOrEmpty()) {
                Toast.makeText(this, "Please enter both tree name and location", Toast.LENGTH_SHORT).show()
            } else {
                // Get current date
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val currentDate = dateFormat.format(Date())
                
                // Create LogEntry object
                val logEntry = LogEntry(
                    name = treeName,
                    location = location,
                    date = currentDate,
                    notes = notes
                )
                
                // Insert into database
                val id = dbHelper.insertLog(logEntry)
                if (id > 0) {
                    Toast.makeText(this, "Log saved successfully", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, "Failed to save log", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroy() {
        dbHelper.close()
        super.onDestroy()
    }
}