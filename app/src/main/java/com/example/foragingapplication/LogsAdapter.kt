package com.example.foragingapp.ui

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.foragingapp.R
import com.example.foragingapp.model.LogEntry

class LogsAdapter(
    private val items: List<LogEntry>,
) : RecyclerView.Adapter<LogsAdapter.VH>() {

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.txtTitle)
        val subtitle: TextView = itemView.findViewById(R.id.txtSubtitle)
        val thumb: ImageView = itemView.findViewById(R.id.imgThumb)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_log, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.title.text = item.name
        holder.subtitle.text = "${item.date} • ${item.location}"
        if (item.imageUri.isNotBlank()) holder.thumb.setImageURI(Uri.parse(item.imageUri))
        else holder.thumb.setImageResource(android.R.drawable.ic_menu_myplaces)
    }

    override fun getItemCount() = items.size
}