package com.example.foragingapp.model

data class LogEntry(
    val id: Long = 0,
    val name: String,
    val location: String,
    val date: String,
    val notes: String = "",
    val imageUri: String = "",
    val lat: Double? = null,
    val lng: Double? = null,
)