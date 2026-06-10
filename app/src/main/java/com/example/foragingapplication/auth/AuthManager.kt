package com.example.foragingapp.auth

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class AppUser(
    val id: String,
    val fullName: String,
    val email: String,
    val createdAt: String
)

object AuthManager {
    private const val PREFS = "foraging_auth"
    private const val KEY_SESSION_ID = "session_id"
    private const val KEY_SESSION_NAME = "session_name"
    private const val KEY_SESSION_EMAIL = "session_email"
    private const val KEY_SESSION_CREATED = "session_created"
    private const val KEY_ACCOUNT_ID = "account_id"
    private const val KEY_ACCOUNT_NAME = "account_name"
    private const val KEY_ACCOUNT_EMAIL = "account_email"
    private const val KEY_ACCOUNT_PASSWORD = "account_password"
    private const val KEY_ACCOUNT_CREATED = "account_created"

    private val legacyKeys = listOf("id", "name", "email", "password", "created")

    fun currentUser(context: Context): AppUser? {
        migrateLegacyAccountIfNeeded(context)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val id = prefs.getString(KEY_SESSION_ID, "").orEmpty()
        if (id.isBlank()) return null
        return AppUser(
            id = id,
            fullName = prefs.getString(KEY_SESSION_NAME, "").orEmpty(),
            email = prefs.getString(KEY_SESSION_EMAIL, "").orEmpty(),
            createdAt = prefs.getString(KEY_SESSION_CREATED, "").orEmpty()
        )
    }

    fun isSignedIn(context: Context): Boolean = currentUser(context) != null

    fun register(context: Context, fullName: String, email: String, password: String): Boolean {
        if (fullName.isBlank() || email.isBlank() || password.length < 4) return false
        val created = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val id = UUID.randomUUID().toString()
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_ACCOUNT_ID, id)
            .putString(KEY_ACCOUNT_NAME, fullName.trim())
            .putString(KEY_ACCOUNT_EMAIL, normalizedEmail(email))
            .putString(KEY_ACCOUNT_PASSWORD, password)
            .putString(KEY_ACCOUNT_CREATED, created)
            .putString(KEY_SESSION_ID, id)
            .putString(KEY_SESSION_NAME, fullName.trim())
            .putString(KEY_SESSION_EMAIL, normalizedEmail(email))
            .putString(KEY_SESSION_CREATED, created)
            .removeLegacyKeys()
            .apply()
        return true
    }

    fun login(context: Context, email: String, password: String): Boolean {
        migrateLegacyAccountIfNeeded(context)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val matches = prefs.getString(KEY_ACCOUNT_EMAIL, "") == normalizedEmail(email) &&
            prefs.getString(KEY_ACCOUNT_PASSWORD, "") == password
        if (!matches) return false

        prefs.edit()
            .putString(KEY_SESSION_ID, prefs.getString(KEY_ACCOUNT_ID, "").orEmpty())
            .putString(KEY_SESSION_NAME, prefs.getString(KEY_ACCOUNT_NAME, "").orEmpty())
            .putString(KEY_SESSION_EMAIL, prefs.getString(KEY_ACCOUNT_EMAIL, "").orEmpty())
            .putString(KEY_SESSION_CREATED, prefs.getString(KEY_ACCOUNT_CREATED, "").orEmpty())
            .apply()
        return true
    }

    fun updateProfile(context: Context, fullName: String, email: String, password: String? = null): Boolean {
        if (fullName.isBlank() || email.isBlank()) return false
        val editor = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_ACCOUNT_NAME, fullName.trim())
            .putString(KEY_ACCOUNT_EMAIL, normalizedEmail(email))
            .putString(KEY_SESSION_NAME, fullName.trim())
            .putString(KEY_SESSION_EMAIL, normalizedEmail(email))
        if (!password.isNullOrBlank()) editor.putString(KEY_ACCOUNT_PASSWORD, password)
        editor.apply()
        return true
    }

    fun signOut(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .remove(KEY_SESSION_ID)
            .remove(KEY_SESSION_NAME)
            .remove(KEY_SESSION_EMAIL)
            .remove(KEY_SESSION_CREATED)
            .apply()
    }

    private fun migrateLegacyAccountIfNeeded(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getString(KEY_ACCOUNT_ID, "").orEmpty().isNotBlank()) return

        val legacyId = prefs.getString("id", "").orEmpty()
        val legacyEmail = prefs.getString("email", "").orEmpty()
        val legacyPassword = prefs.getString("password", "").orEmpty()
        if (legacyId.isBlank() || legacyEmail.isBlank() || legacyPassword.isBlank()) return

        val legacyName = prefs.getString("name", "").orEmpty()
        val legacyCreated = prefs.getString("created", "").orEmpty()
        prefs.edit()
            .putString(KEY_ACCOUNT_ID, legacyId)
            .putString(KEY_ACCOUNT_NAME, legacyName)
            .putString(KEY_ACCOUNT_EMAIL, normalizedEmail(legacyEmail))
            .putString(KEY_ACCOUNT_PASSWORD, legacyPassword)
            .putString(KEY_ACCOUNT_CREATED, legacyCreated)
            .putString(KEY_SESSION_ID, legacyId)
            .putString(KEY_SESSION_NAME, legacyName)
            .putString(KEY_SESSION_EMAIL, normalizedEmail(legacyEmail))
            .putString(KEY_SESSION_CREATED, legacyCreated)
            .removeLegacyKeys()
            .apply()
    }

    private fun normalizedEmail(email: String): String = email.trim().lowercase(Locale.US)

    private fun android.content.SharedPreferences.Editor.removeLegacyKeys(): android.content.SharedPreferences.Editor {
        legacyKeys.forEach { remove(it) }
        return this
    }
}
