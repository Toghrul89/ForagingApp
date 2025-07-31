package com.example.foragingapplication

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LogEntryActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log_entry)

        // Connect UI elements
        val itemNameEditText = findViewById<EditText>(R.id.editTextItemName)
        val locationEditText = findViewById<EditText>(R.id.editTextLocation)
        val notesEditText = findViewById<EditText>(R.id.editTextNotes)
        val dateEditText = findViewById<EditText>(R.id.editTextDate)
        val saveButton = findViewById<Button>(R.id.buttonSaveLog)

        // Handle button click
        saveButton.setOnClickListener {
            val itemName = itemNameEditText.text.toString().trim()
            val location = locationEditText.text.toString().trim()
            val notes = notesEditText.text.toString().trim()
            val date = dateEditText.text.toString().trim()

            if (itemName.isNotEmpty() && location.isNotEmpty() && date.isNotEmpty()) {
                val message = "Saved: $itemName at $location on $date"
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                // Here you can later add: save to database or file
            } else {
                Toast.makeText(this, "Please fill in item name, location and date", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
