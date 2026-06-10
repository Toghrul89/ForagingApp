package com.example.foragingapp.ui

import android.content.Context

object DisclaimerManager {
    private const val PREFS = "zogal_disclaimer"
    private const val KEY_ACCEPTED = "accepted_safety_disclaimer_v2"

    fun hasAccepted(context: Context): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_ACCEPTED, false)
    }

    fun accept(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ACCEPTED, true)
            .apply()
    }
}
