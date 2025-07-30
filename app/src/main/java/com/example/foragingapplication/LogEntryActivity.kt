package com.example.foragingapplication

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class LogEntryActivity : AppCompatActivity() {

    private lateinit var plantNameEditText: EditText
    private lateinit var notesEditText: EditText
    private lateinit var dateTextView: TextView
    private lateinit var saveButton: Button

    private var selectedDate: Calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log_entry)

        plantNameEditText = findViewById(R.id.editTextPlantName)
        notesEditText = findViewById(R.id.editTextNotes)
        dateTextView = findViewById(R.id.textViewDate)
        saveButton = findViewById(R.id.buttonSave)

        updateDateInView()

        dateTextView.setOnClickListener {
            DatePickerDialog(this,
                { _, year, month, day ->
                    selectedDate.set(Calendar.YEAR, year)
                    selectedDate.set(Calendar.MONTH, month)
                    selectedDate.set(Calendar.DAY_OF_MONTH, day)
                    updateDateInView()
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        saveButton.setOnClickListener {
            val plantName = plantNameEditText.text.toString().trim()
            val notes = notesEditText.text.toString().trim()
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(selectedDate.time)

            if (plantName.isEmpty()) {
                Toast.makeText(this, "Please enter a plant name", Toast.LENGTH_SHORT).show()
            } else {
                // TODO: Save to database later
                Toast.makeText(this, "Saved: $plantName on $date", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun updateDateInView() {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        dateTextView.text = format.format(selectedDate.time)
    }
}
