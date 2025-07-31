package com.example.foragingapplication

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class LogEntryActivity : AppCompatActivity() {

    private lateinit var editTextItemName: EditText
    private lateinit var editTextLocation: EditText
    private lateinit var editTextNotes: EditText
    private lateinit var editTextDate: EditText
    private lateinit var buttonSaveLog: Button
    private lateinit var buttonSelectImage: Button
    private lateinit var imageViewSelected: ImageView
    private var selectedImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log_entry)

        // Initialize views
        editTextItemName = findViewById(R.id.editTextItemName)
        editTextLocation = findViewById(R.id.editTextLocation)
        editTextNotes = findViewById(R.id.editTextNotes)
        editTextDate = findViewById(R.id.editTextDate)
        buttonSaveLog = findViewById(R.id.buttonSaveLog)
        buttonSelectImage = findViewById(R.id.buttonSelectImage)
        imageViewSelected = findViewById(R.id.imageViewSelected)

        // Image picker
        buttonSelectImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, 1001)
        }

        // Save log
        buttonSaveLog.setOnClickListener {
            saveLog()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001 && resultCode == Activity.RESULT_OK && data != null) {
            selectedImageUri = data.data
            imageViewSelected.setImageURI(selectedImageUri)
        }
    }

    private fun saveLog() {
        val itemName = editTextItemName.text.toString().trim()
        val location = editTextLocation.text.toString().trim()
        val notes = editTextNotes.text.toString().trim()
        val date = editTextDate.text.toString().trim()
        val image = selectedImageUri?.toString() ?: "No image"

        if (itemName.isEmpty() || location.isEmpty() || date.isEmpty()) {
            Toast.makeText(this, "Please fill out required fields", Toast.LENGTH_SHORT).show()
            return
        }

        val logEntry = """
            Name: $itemName
            Location: $location
            Notes: $notes
            Date: $date
            Image URI: $image

        """.trimIndent()

        val sharedPreferences = getSharedPreferences("ForagingLogs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val logId = System.currentTimeMillis().toString()
        editor.putString(logId, logEntry)
        editor.apply()

        Toast.makeText(this, "Log saved!", Toast.LENGTH_SHORT).show()

        // Clear fields
        editTextItemName.text.clear()
        editTextLocation.text.clear()
        editTextNotes.text.clear()
        editTextDate.text.clear()
        imageViewSelected.setImageResource(0)
        selectedImageUri = null
    }
}
