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

        /** Returns an emoji that represents the fruit type for use as image placeholder */
        fun emojiForType(treeType: String): String = when (treeType.lowercase()) {
            "cornelian cherry" -> "🌿"
            "apple", "crabapple" -> "🍎"
            "cherry" -> "🍒"
            "pear" -> "🍐"
            "plum" -> "🫐"
            "mulberry", "blackberry", "elderberry" -> "🫐"
            "fig" -> "🍈"
            "walnut", "hazelnut" -> "🌰"
            "quince" -> "🍋"
            "persimmon" -> "🟠"
            "hawthorn", "serviceberry" -> "🌸"
            else -> "🌿"
        }

        fun ripenessColor(ripeness: String): Int = when (ripeness) {
            "Peak" -> 0xFF2E7D32.toInt()          // dark green
            "Almost Ready" -> 0xFFF9A825.toInt()  // amber
            "Unripe" -> 0xFF78909C.toInt()         // grey-blue
            "Overripe" -> 0xFF6D4C41.toInt()       // brown
            else -> 0xFF8A9E8C.toInt()
        }
    }

    inner class LogViewHolder(val binding: ItemLogBinding) :
        RecyclerView.ViewHolder(binding.root)

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
            tvLogNotes.text = log.notes
            tvLogNotes.visibility = if (log.notes.isNotEmpty()) View.VISIBLE else View.GONE
            tvTreeType.text = log.treeType
            tvSeason.text = if (log.season.isNotEmpty()) "· ${log.season}" else ""

            // Ripeness badge
            if (log.ripeness.isNotEmpty()) {
                tvRipeness.text = log.ripeness
                tvRipeness.setTextColor(ripenessColor(log.ripeness))
                tvRipeness.visibility = View.VISIBLE
            } else {
                tvRipeness.visibility = View.GONE
            }

            // Star rating display (read-only in list)
            tvRating.text = "★".repeat(log.rating) + "☆".repeat(5 - log.rating)
            tvRating.visibility = if (log.rating > 0) View.VISIBLE else View.GONE

            btnFavorite.text = if (log.isFavorite) "★" else "☆"
            btnFavorite.setOnClickListener { onFavorite(log) }

            // Photo or animated emoji placeholder
            if (log.imageUri.isNotEmpty()) {
                ivLogThumbnail.clearColorFilter()
                Glide.with(root.context)
                    .load(Uri.parse(log.imageUri))
                    .centerCrop()
                    .into(ivLogThumbnail)
                ivLogThumbnail.visibility = View.VISIBLE
                tvEmojiPlaceholder.visibility = View.GONE
            } else {
                ivLogThumbnail.setImageDrawable(null)
                ivLogThumbnail.visibility = View.INVISIBLE
                tvEmojiPlaceholder.text = emojiForType(log.treeType)
                tvEmojiPlaceholder.visibility = View.VISIBLE
                // Pulse animation on the emoji placeholder
                val anim = android.view.animation.AnimationUtils.loadAnimation(
                    root.context, android.R.anim.fade_in
                )
                tvEmojiPlaceholder.startAnimation(anim)
            }

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
