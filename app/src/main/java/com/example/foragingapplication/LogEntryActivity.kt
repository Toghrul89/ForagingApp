package com.example.foragingapp

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.foragingapp.model.LogEntry
import java.text.SimpleDateFormat
import java.util.*

class LogEntryActivity : AppCompatActivity() {
    private lateinit var dbHelper: LogDatabaseHelper
    private var selectedImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log_entry)

        dbHelper = LogDatabaseHelper(this)

        val nameInput = findViewById<EditText>(R.id.editTextItemName)
        val locationInput = findViewById<EditText>(R.id.editTextLocation)
        val dateInput = findViewById<EditText>(R.id.editTextDate)
        val notesInput = findViewById<EditText>(R.id.editTextNotes)
        val imageView = findViewById<ImageView>(R.id.imageViewSelected)

        findViewById<Button>(R.id.buttonSelectImage).setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
            startActivityForResult(intent, 1001)
        }

        findViewById<Button>(R.id.buttonSaveLog).setOnClickListener {
            val entry = LogEntry(
                name = nameInput.text.toString(),
                location = locationInput.text.toString(),
                date = dateInput.text.toString(),
                notes = notesInput.text.toString(),
                imageUri = selectedImageUri?.toString() ?: ""
            )
            dbHelper.insertLog(entry)
            Toast.makeText(this, "Log saved!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001 && resultCode == Activity.RESULT_OK) {
            selectedImageUri = data?.data
            findViewById<ImageView>(R.id.imageViewSelected).setImageURI(selectedImageUri)
        }
    }
}