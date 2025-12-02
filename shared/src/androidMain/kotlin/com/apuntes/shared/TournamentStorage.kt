package com.apuntes.shared

import android.content.Context
import android.preference.PreferenceManager

actual object TournamentStorage {

    private lateinit var appContext: Context
    private const val KEY_TOURNAMENT = "tournament_json"

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    actual fun saveTournament(jsonString: String) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(appContext)
        prefs.edit().putString(KEY_TOURNAMENT, jsonString).apply()
    }

    actual fun loadTournament(): String? {
        val prefs = PreferenceManager.getDefaultSharedPreferences(appContext)
        return prefs.getString(KEY_TOURNAMENT, null)
    }
}
