package com.example.foragingapp

import android.os.Bundle
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity

class LogListActivity : AppCompatActivity() {

    private lateinit var dbHelper: LogDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log_list)

        dbHelper = LogDatabaseHelper(this)

        val listView = findViewById<ListView>(R.id.logListView)
        val logs = dbHelper.getAllLogs()

        val adapter = LogsAdapter(this, logs)
        listView.adapter = adapter
    }

    override fun onDestroy() {
        dbHelper.close()
        super.onDestroy()
    }
}
