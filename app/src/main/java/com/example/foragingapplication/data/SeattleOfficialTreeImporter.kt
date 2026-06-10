package com.example.foragingapp.data

import com.example.foragingapp.model.LogEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SeattleOfficialTreeImporter {
    private const val SDOT_TREES_URL =
        "https://data.seattle.gov/resource/gapw-b2rf.json?\$limit=5000"

    private val edibleKeywords = listOf(
        "apple", "crabapple", "pear", "cherry", "plum", "fig", "mulberry",
        "blackberry", "blueberry", "elderberry", "serviceberry", "hawthorn",
        "hazelnut", "filbert", "persimmon", "quince", "cornelian"
    )

    suspend fun importIfNeeded(repository: LogRepository): Int = withContext(Dispatchers.IO) {
        if (repository.countOfficialTrees() > 0) return@withContext 0
        val entries = fetchOfficialEntries()
        if (entries.isEmpty()) return@withContext 0
        repository.insertAll(entries).count { it > 0L }
    }

    private fun fetchOfficialEntries(): List<LogEntry> {
        val connection = (URL(SDOT_TREES_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 12_000
            readTimeout = 20_000
        }
        return try {
            if (connection.responseCode !in 200..299) return emptyList()
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            parseEntries(JSONArray(body))
        } catch (_: Exception) {
            emptyList()
        } finally {
            connection.disconnect()
        }
    }

    private fun parseEntries(array: JSONArray): List<LogEntry> {
        val importedAt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
        val entries = mutableListOf<LogEntry>()
        val seen = mutableSetOf<String>()

        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val commonName = firstText(item, "common_name", "common", "commonname", "tree_common", "spc_common", "species", "unitdesc")
            val scientificName = firstText(item, "scientific_name", "scientific", "botanical_name", "spc_latin", "genus_species")
            val searchable = "$commonName $scientificName".lowercase(Locale.US)
            if (edibleKeywords.none { searchable.contains(it) }) continue

            val lat = firstDouble(item, "latitude", "lat", "y") ?: item.optJSONObject("location")?.optDouble("latitude")
            val lng = firstDouble(item, "longitude", "lon", "lng", "x") ?: item.optJSONObject("location")?.optDouble("longitude")
            if (lat == null || lng == null || lat.isNaN() || lng.isNaN()) continue
            if (lat !in 47.49..47.80 || lng !in -122.47..-122.24) continue

            val name = cleanFruitName(commonName.ifBlank { scientificName })
            if (name.isBlank()) continue
            val key = "${name.lowercase(Locale.US)}:${String.format(Locale.US, "%.5f", lat)}:${String.format(Locale.US, "%.5f", lng)}"
            if (!seen.add(key)) continue

            entries.add(
                LogEntry(
                    name = name,
                    location = firstText(item, "address", "unitdesc", "street", "site_name").ifBlank { "Seattle public right-of-way" },
                    date = importedAt,
                    notes = "Imported from Seattle public tree inventory. Verify access and fruit identity before foraging.",
                    lat = lat,
                    lng = lng,
                    treeType = fruitType(name, scientificName),
                    season = seasonFor(name),
                    wikipediaUrl = wikipediaUrl(name),
                    creatorName = "Seattle Urban Forestry Dataset",
                    createdAt = importedAt,
                    isPublic = true,
                    verificationStatus = "Official Dataset",
                    dataSource = "OFFICIAL",
                    scientificName = scientificName,
                    accessType = "Unknown",
                    fruitCategory = fruitCategory(name),
                    sourceLabel = "Seattle Urban Forestry Dataset"
                )
            )
        }
        return entries.take(750)
    }

    private fun firstText(item: JSONObject, vararg keys: String): String {
        keys.forEach { key ->
            val value = item.optString(key).trim()
            if (value.isNotBlank() && value != "null") return value
        }
        return ""
    }

    private fun firstDouble(item: JSONObject, vararg keys: String): Double? {
        keys.forEach { key ->
            if (item.has(key)) {
                val value = item.optString(key)
                value.toDoubleOrNull()?.let { return it }
            }
        }
        return null
    }

    private fun cleanFruitName(value: String): String {
        val lower = value.lowercase(Locale.US)
        return when {
            "cornelian" in lower -> "Cornelian Cherry"
            "crabapple" in lower || "crab apple" in lower -> "Crabapple"
            "apple" in lower -> "Apple"
            "pear" in lower -> "Pear"
            "cherry" in lower -> "Cherry"
            "plum" in lower -> "Plum"
            "fig" in lower -> "Fig"
            "mulberry" in lower -> "Mulberry"
            "blackberry" in lower -> "Blackberry"
            "blueberry" in lower -> "Blueberry"
            "elderberry" in lower -> "Elderberry"
            "serviceberry" in lower -> "Serviceberry"
            "hazelnut" in lower || "filbert" in lower -> "Hazelnut"
            "persimmon" in lower -> "Persimmon"
            "quince" in lower -> "Quince"
            "hawthorn" in lower -> "Hawthorn"
            else -> value.trim()
        }
    }

    private fun fruitType(name: String, scientificName: String): String = cleanFruitName("$name $scientificName")

    private fun fruitCategory(name: String): String {
        val lower = name.lowercase(Locale.US)
        return when {
            lower.contains("berry") -> "Berry"
            lower.contains("cherry") || lower.contains("plum") -> "Stone Fruit"
            lower.contains("hazelnut") -> "Nut"
            else -> "Fruit Tree"
        }
    }

    private fun seasonFor(name: String): String {
        val lower = name.lowercase(Locale.US)
        return when {
            lower.contains("serviceberry") || lower.contains("cherry") -> "Summer"
            lower.contains("blackberry") || lower.contains("blueberry") || lower.contains("elderberry") -> "Summer"
            lower.contains("apple") || lower.contains("pear") || lower.contains("persimmon") || lower.contains("quince") -> "Autumn"
            lower.contains("hazelnut") -> "Autumn"
            else -> "Seasonal"
        }
    }

    private fun wikipediaUrl(name: String): String {
        val canonical = when (name.lowercase(Locale.US)) {
            "cornelian cherry" -> "Cornelian_cherry"
            "serviceberry" -> "Amelanchier"
            "hazelnut" -> "Hazelnut"
            "hawthorn" -> "Crataegus"
            else -> URLEncoder.encode(name, "UTF-8").replace("+", "_")
        }
        return "https://en.wikipedia.org/wiki/$canonical"
    }
}
