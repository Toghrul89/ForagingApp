package com.example.foragingapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

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
        val logList = databaseHelper.getAllLogs()

        adapter = LogsAdapter(logList)
        logsRecyclerView.adapter = adapter

        backButton.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}
