package com.example.foragingapp.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "logs")
data class LogEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Display name the user gave this spot */
    val name: String,

    /** Human-readable address / area */
    val location: String,

    /** ISO date-time string: "yyyy-MM-dd HH:mm" */
    val date: String,

    /** Free-form notes */
    val notes: String = "",

    /** Content URI string pointing to a saved photo */
    val imageUri: String = "",

    /** GPS latitude — null if not captured */
    val lat: Double? = null,

    /** GPS longitude — null if not captured */
    val lng: Double? = null,

    /** Category tag, e.g. "Cornelian Cherry", "Apple", etc. */
    val treeType: String = "Other",

    /** Best season to harvest */
    val season: String = "",

    /** User-starred for quick access */
    val isFavorite: Boolean = false,

    /** Ripeness when visited: "Unripe" | "Almost Ready" | "Peak" | "Overripe" */
    val ripeness: String = "",

    /** User rating 1-5, 0 = not rated */
    val rating: Int = 0
)
