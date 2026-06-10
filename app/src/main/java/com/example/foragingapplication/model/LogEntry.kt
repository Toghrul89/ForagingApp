package com.example.foragingapp.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Core data model for a fruit-tree spot.
 * Core data model for a fruit-tree spot.
 */
@Entity(tableName = "logs")
data class LogEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Display name the user gave this spot, e.g. "Big Apple by the creek" */
    val name: String,

    /** Human-readable address or area, e.g. "Seattle, WA" */
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

    /** Category tag, e.g. "Apple", "Cherry", "Pear", "Plum", "Mulberry", "Other" */
    val treeType: String = "Other",

    /** Best season to harvest, e.g. "Summer", "Autumn", "Spring", "Year-round" */
    val season: String = "",

    /** User-starred for quick access */
    val isFavorite: Boolean = false,

    /** Optional direct Wikipedia/source URL supplied by the user */
    val wikipediaUrl: String = "",

    /** User id of the person who added this tree. Blank means legacy/public. */
    val creatorUserId: String = "",

    /** Display name of the person who added this tree. */
    val creatorName: String = "",

    /** Date the tree was first added. */
    val createdAt: String = "",

    /** Whether this tree is visible to other users when cloud sync is added. */
    val isPublic: Boolean = true,

    /** Verification state: Needs verification, User verified, Community confirmed */
    val verificationStatus: String = "Needs verification",

    /** Simple local moderation flag until a backend moderation queue exists. */
    val isReported: Boolean = false,

    /** OFFICIAL for Seattle public-data imports, COMMUNITY for user contributions. */
    val dataSource: String = "COMMUNITY",

    /** Botanical/scientific name when known. */
    val scientificName: String = "",

    /** Public, Private, or Unknown access label. */
    val accessType: String = "Unknown",

    /** Fruit category used for markers and filtering, e.g. Apple, Berry, Stone Fruit. */
    val fruitCategory: String = "Fruit",

    /** Professional contributor/source label shown in cards and details. */
    val sourceLabel: String = ""
)
