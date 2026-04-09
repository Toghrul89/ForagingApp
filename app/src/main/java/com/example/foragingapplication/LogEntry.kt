package com.example.foragingapp.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "logs")
data class LogEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val location: String,
    val date: String,
    val notes: String = "",
    val imageUri: String = "",
    val lat: Double? = null,
    val lng: Double? = null,
)