package com.example.foragingapp

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.foragingapp.model.LogEntry

class LogsAdapter(private val logs: List<LogEntry>) : RecyclerView.Adapter<LogsAdapter.LogViewHolder>() {

    class LogViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvLogName)
        val tvLocation: TextView = view.findViewById(R.id.tvLogLocation)
        val tvDate: TextView = view.findViewById(R.id.tvLogDate)
        val tvNotes: TextView = view.findViewById(R.id.tvLogNotes)
        val ivThumbnail: ImageView = view.findViewById(R.id.ivLogThumbnail)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_log, parent, false)
        return LogViewHolder(view)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        val log = logs[position]
        holder.tvName.text = log.name
        holder.tvLocation.text = log.location
        holder.tvDate.text = log.date
        holder.tvNotes.text = if (log.notes.isNotEmpty()) log.notes else "No notes"
        
        if (log.imageUri.isNotEmpty()) {
            holder.ivThumbnail.setImageURI(Uri.parse(log.imageUri))
            holder.ivThumbnail.visibility = ImageView.VISIBLE
        } else {
            holder.ivThumbnail.visibility = ImageView.GONE
        }
        
        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, LogEntryActivity::class.java)
            intent.putExtra("LOG_ID", log.id)
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = logs.size
}