package com.example.foragingapp

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.foragingapp.data.LogDatabaseHelper

class ViewLogsActivity : AppCompatActivity() {

    private lateinit var logsRecyclerView: RecyclerView
    private lateinit var backButton: Button
    private lateinit var adapter: LogsAdapter
    private lateinit var databaseHelper: LogDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_logs)

        logsRecyclerView = findViewById(R.id.rvLogs)
        backButton = findViewById(R.id.btnBack)

        logsRecyclerView.layoutManager = LinearLayoutManager(this)

        databaseHelper = LogDatabaseHelper(this)
        loadLogs()

        backButton.setOnClickListener {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        loadLogs()
    }

    private fun loadLogs() {
        val logList = databaseHelper.getAllLogs()
        adapter = LogsAdapter(logList)
        logsRecyclerView.adapter = adapter
    }
}