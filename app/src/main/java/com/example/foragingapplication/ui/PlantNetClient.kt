package com.example.foragingapp.ui

import android.content.Context
import android.net.Uri
import com.example.foragingapp.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

object PlantNetClient {
    private const val LINE_END = "\r\n"
    private val fruitWords = setOf(
        "apple", "pear", "plum", "cherry", "cornelian", "dogwood", "mulberry",
        "fig", "quince", "persimmon", "serviceberry", "hawthorn", "elderberry",
        "blackberry", "raspberry", "salmonberry", "huckleberry", "crabapple"
    )

    suspend fun identify(context: Context, uri: Uri): IdentificationResult? = withContext(Dispatchers.IO) {
        if (BuildConfig.PLANTNET_API_KEY.isBlank()) return@withContext null

        val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
            input.readBytes()
        } ?: return@withContext null
        val mime = context.contentResolver.getType(uri)?.takeIf { it == "image/png" || it == "image/jpeg" }
            ?: "image/jpeg"
        val extension = if (mime == "image/png") "png" else "jpg"
        val boundary = "ZogalBoundary${System.currentTimeMillis()}"
        val url = URL(
            "https://my-api.plantnet.org/v2/identify/${BuildConfig.PLANTNET_PROJECT}" +
                "?api-key=${BuildConfig.PLANTNET_API_KEY}&lang=en&nb-results=3"
        )

        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20_000
            readTimeout = 30_000
            doInput = true
            doOutput = true
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        }

        connection.outputStream.use { output ->
            fun write(value: String) = output.write(value.toByteArray(Charsets.UTF_8))
            write("--$boundary$LINE_END")
            write("Content-Disposition: form-data; name=\"organs\"$LINE_END$LINE_END")
            write("fruit$LINE_END")
            write("--$boundary$LINE_END")
            write("Content-Disposition: form-data; name=\"images\"; filename=\"zogal.$extension\"$LINE_END")
            write("Content-Type: $mime$LINE_END$LINE_END")
            output.write(bytes)
            write(LINE_END)
            write("--$boundary--$LINE_END")
        }

        val responseCode = connection.responseCode
        val body = if (responseCode in 200..299) {
            connection.inputStream.bufferedReader().use { it.readText() }
        } else {
            connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
        }
        connection.disconnect()
        if (responseCode !in 200..299 || body.isBlank()) return@withContext null

        parseResult(body)
    }

    private fun parseResult(body: String): IdentificationResult? {
        val json = JSONObject(body)
        val results = json.optJSONArray("results") ?: return null
        if (results.length() == 0) return null

        val top = results.getJSONObject(0)
        val score = top.optDouble("score", 0.0)
        val species = top.optJSONObject("species") ?: JSONObject()
        val commonNames = species.optJSONArray("commonNames")
        val common = commonNames?.optString(0).orEmpty()
        val scientific = species.optString("scientificNameWithoutAuthor")
            .ifBlank { species.optString("scientificName") }
        val name = common.ifBlank { scientific }.ifBlank { json.optString("bestMatch", "Possible fruit tree") }
        val fruitFocused = isFruitRelated(name) || isFruitRelated(scientific)
        val percent = (score * 100).toInt().coerceIn(0, 100)
        val description = buildString {
            append("PlantNet suggested ")
            append(name)
            if (scientific.isNotBlank() && scientific != name) append(" ($scientific)")
            append(". ")
            append(
                if (fruitFocused) {
                    "This appears relevant to urban fruit discovery, but still needs manual verification."
                } else {
                    "This may not be a fruit tree. Add it only if you can verify it fits Zogal."
                }
            )
        }
        return IdentificationResult(
            name = name,
            description = description,
            confidence = "$percent%",
            scientificName = scientific,
            season = seasonFor(name),
            category = categoryFor(name),
            wikipediaUrl = wikipediaUrl(name)
        )
    }

    private fun isFruitRelated(value: String): Boolean {
        val lower = value.lowercase(Locale.US)
        return fruitWords.any { lower.contains(it) }
    }

    private fun seasonFor(name: String): String {
        val lower = name.lowercase(Locale.US)
        return when {
            lower.contains("apple") || lower.contains("pear") || lower.contains("persimmon") || lower.contains("quince") -> "Autumn"
            lower.contains("hazelnut") -> "Autumn"
            else -> "Summer"
        }
    }

    private fun categoryFor(name: String): String {
        val lower = name.lowercase(Locale.US)
        return when {
            lower.contains("berry") -> "Berry"
            lower.contains("cherry") || lower.contains("plum") -> "Stone Fruit"
            lower.contains("hazelnut") -> "Nut"
            else -> "Fruit Tree"
        }
    }

    private fun wikipediaUrl(name: String): String {
        val canonical = when (name.lowercase(Locale.US)) {
            "cornelian cherry" -> "Cornelian_cherry"
            "serviceberry" -> "Amelanchier"
            "mulberry" -> "Morus_(plant)"
            else -> URLEncoder.encode(name, "UTF-8").replace("+", "_")
        }
        return "https://en.wikipedia.org/wiki/$canonical"
    }
}
