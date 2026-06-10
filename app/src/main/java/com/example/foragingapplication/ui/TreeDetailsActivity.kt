package com.example.foragingapp.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.foragingapp.LogViewModel
import com.example.foragingapp.auth.AuthManager
import com.example.foragingapp.databinding.ActivityTreeDetailsBinding
import com.example.foragingapp.model.LogEntry
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class TreeDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTreeDetailsBinding
    private val viewModel: LogViewModel by viewModels()
    private var currentLog: LogEntry? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTreeDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        val logId = intent.getLongExtra("LOG_ID", -1L)
        lifecycleScope.launch {
            val log = if (logId != -1L) viewModel.getLogById(logId) else findLogFromExtras()
            if (log == null) {
                Toast.makeText(this@TreeDetailsActivity, "Tree details not found", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                showLog(log)
            }
        }
    }

    private suspend fun findLogFromExtras(): LogEntry? {
        val name = intent.getStringExtra("TREE_NAME") ?: return null
        val lat = intent.getDoubleExtra("TREE_LAT", Double.MIN_VALUE)
        val lng = intent.getDoubleExtra("TREE_LNG", Double.MIN_VALUE)
        return viewModel.getAllLogsOnce().firstOrNull { log ->
            log.name == name &&
                (lat == Double.MIN_VALUE || log.lat == null || kotlin.math.abs(log.lat - lat) < 0.00001) &&
                (lng == Double.MIN_VALUE || log.lng == null || kotlin.math.abs(log.lng - lng) < 0.00001)
        }
    }

    private fun showLog(log: LogEntry) {
        currentLog = log
        binding.toolbar.title = log.name
        binding.textName.text = log.name

        val meta = buildList {
            if (log.scientificName.isNotBlank()) add(log.scientificName)
            if (log.treeType.isNotBlank()) add(log.treeType)
            if (log.season.isNotBlank()) add(log.season)
            if (log.location.isNotBlank()) add("Neighborhood: ${log.location}")
            if (log.sourceLabel.isNotBlank()) add(log.sourceLabel)
            else if (log.creatorName.isNotBlank()) add("Added by ${log.creatorName}")
            val created = log.createdAt.ifBlank { log.date }
            if (created.isNotBlank()) add("Created: $created")
        }.joinToString("\n")
        binding.textMeta.text = meta

        binding.textNotes.text = log.notes.ifBlank { "No description added yet." }
        binding.textTrust.text = buildString {
            append(if (log.dataSource == "OFFICIAL") "Official Dataset" else "Community Contribution")
            append(" • ")
            append(log.verificationStatus)
            append(" • ")
            append(log.accessType.ifBlank { if (log.isPublic) "Public" else "Private" })
            if (log.isReported) append(" • Reported")
        }

        if (log.imageUri.isNotBlank()) {
            binding.imageTree.visibility = View.VISIBLE
            Glide.with(this).load(Uri.parse(log.imageUri)).centerCrop().into(binding.imageTree)
        } else {
            binding.imageTree.visibility = View.GONE
        }

        binding.buttonWikipedia.visibility = if (hasWikipediaDirection(log)) View.VISIBLE else View.GONE
        binding.buttonWikipedia.setOnClickListener { openWikipedia(log) }
        val canManage = log.dataSource != "OFFICIAL" &&
            AuthManager.currentUser(this)?.id == log.creatorUserId &&
            log.creatorUserId.isNotBlank()
        binding.buttonEdit.visibility = if (canManage) View.VISIBLE else View.GONE
        binding.buttonDelete.visibility = if (canManage) View.VISIBLE else View.GONE
        binding.buttonEdit.setOnClickListener {
            startActivity(Intent(this, LogEntryActivity::class.java).putExtra("LOG_ID", log.id))
        }
        binding.buttonReport.setOnClickListener { reportIssue(log) }
        binding.buttonDelete.setOnClickListener { confirmDelete(log) }
    }

    private fun openWikipedia(log: LogEntry) {
        val url = when {
            log.wikipediaUrl.startsWith("http://") || log.wikipediaUrl.startsWith("https://") -> log.wikipediaUrl
            log.name.isNotBlank() -> {
                val encodedName = URLEncoder.encode(log.name, StandardCharsets.UTF_8.toString())
                "https://en.wikipedia.org/wiki/Special:Search?search=$encodedName"
            }
            else -> return
        }
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    private fun hasWikipediaDirection(log: LogEntry): Boolean {
        return log.wikipediaUrl.startsWith("http://") ||
            log.wikipediaUrl.startsWith("https://") ||
            log.name.isNotBlank()
    }

    private fun confirmDelete(log: LogEntry) {
        AlertDialog.Builder(this)
            .setTitle("Delete this tree?")
            .setMessage("This removes ${log.name} from your saved trees and the map.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.delete(log)
                Toast.makeText(this, "Tree deleted", Toast.LENGTH_SHORT).show()
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun reportIssue(log: LogEntry) {
        val reasons = arrayOf("Incorrect information", "Unsafe identification", "Spam", "Offensive content")
        AlertDialog.Builder(this)
            .setTitle("Report issue")
            .setItems(reasons) { _, which ->
                val reason = reasons[which]
                viewModel.update(log.copy(isReported = true, verificationStatus = "Needs verification"))
                Toast.makeText(this, "Reported: $reason", Toast.LENGTH_SHORT).show()
                showLog(log.copy(isReported = true, verificationStatus = "Needs verification"))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
