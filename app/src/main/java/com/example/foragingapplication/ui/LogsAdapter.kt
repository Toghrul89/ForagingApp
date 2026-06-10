package com.example.foragingapp.ui

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.foragingapp.databinding.ItemLogBinding
import com.example.foragingapp.model.LogEntry

class LogsAdapter(
    private val onEdit: (LogEntry) -> Unit,
    private val onMapClick: (LogEntry) -> Unit,
    private val onFavorite: (LogEntry) -> Unit
) : ListAdapter<LogEntry, LogsAdapter.LogViewHolder>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<LogEntry>() {
            override fun areItemsTheSame(a: LogEntry, b: LogEntry) = a.id == b.id
            override fun areContentsTheSame(a: LogEntry, b: LogEntry) = a == b
        }
    }

    inner class LogViewHolder(val binding: ItemLogBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val binding = ItemLogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LogViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        val log = getItem(position)
        with(holder.binding) {
            tvLogName.text = log.name
            tvLogDate.text = log.date
            tvLogLocation.text = log.location
            tvLogNotes.text = buildString {
                append(if (log.dataSource == "OFFICIAL") "Official Dataset" else "Community Contribution")
                append(" • ")
                append(log.accessType.ifBlank { if (log.isPublic) "Public" else "Private" })
                if (log.sourceLabel.isNotBlank()) {
                    append("\n")
                    append(log.sourceLabel)
                } else if (log.notes.isNotBlank()) {
                    append("\n")
                    append(log.notes)
                }
            }
            tvLogNotes.visibility = View.VISIBLE
            tvTreeType.text = log.treeType
            tvSeason.text = buildString {
                if (log.season.isNotEmpty()) append("· ${log.season}")
                if (log.scientificName.isNotEmpty()) append(" · ${log.scientificName}")
            }

            // Favorite star
            btnFavorite.text = if (log.isFavorite) "★" else "☆"
            btnFavorite.setOnClickListener { onFavorite(log) }

            // Photo thumbnail
            if (log.imageUri.isNotEmpty()) {
                Glide.with(root.context)
                    .load(Uri.parse(log.imageUri))
                    .centerCrop()
                    .into(ivLogThumbnail)
                ivLogThumbnail.visibility = View.VISIBLE
            } else {
                ivLogThumbnail.setImageDrawable(null)
                ivLogThumbnail.visibility = View.INVISIBLE
            }

            // Map chip
            if (log.lat != null && log.lng != null) {
                btnViewOnMap.visibility = View.VISIBLE
                btnViewOnMap.setOnClickListener { onMapClick(log) }
            } else {
                btnViewOnMap.visibility = View.GONE
            }

            root.setOnClickListener { onEdit(log) }
        }
    }

    fun getItemAt(position: Int): LogEntry = getItem(position)
}
