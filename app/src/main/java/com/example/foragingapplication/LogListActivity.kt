package com.example.foragingapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.foragingapp.data.LogDatabaseHelper
import com.example.foragingapp.ui.LogsAdapter

class LogListActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log_list)

        val db = LogDatabaseHelper(this)
        val logs = db.getAllLogs()

        val rv = findViewById<RecyclerView>(R.id.recyclerLogs)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = LogsAdapter(logs)
    }
}