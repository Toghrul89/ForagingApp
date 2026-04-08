package com.example.foragingapp

import android.widget.LinearLayout
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.foragingapp.data.LogDatabaseHelper
import com.google.android.material.appbar.MaterialToolbar

class ViewLogsActivity : AppCompatActivity() {

    private lateinit var logsRecyclerView: RecyclerView
    private lateinit var tvEmpty: LinearLayout
    private lateinit var adapter: LogsAdapter
    private lateinit var databaseHelper: LogDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_logs)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        logsRecyclerView = findViewById(R.id.rvLogs)
        tvEmpty = findViewById(R.id.tvEmpty)
        logsRecyclerView.layoutManager = LinearLayoutManager(this)

        databaseHelper = LogDatabaseHelper(this)
        loadLogs()
    }

    override fun onResume() {
        super.onResume()
        loadLogs()
    }

    private fun loadLogs() {
        val logList = databaseHelper.getAllLogs()
        if (logList.isEmpty()) {
            logsRecyclerView.visibility = View.GONE
            tvEmpty.visibility = View.VISIBLE
        } else {
            logsRecyclerView.visibility = View.VISIBLE
            tvEmpty.visibility = View.GONE
            adapter = LogsAdapter(logList)
            logsRecyclerView.adapter = adapter
        }
    }
}