package com.apuntes.shared

import platform.Foundation.NSUserDefaults

actual object TournamentStorage {
    private const val KEY_TOURNAMENT = "tournament_json"

    actual fun saveTournament(jsonString: String) {
        NSUserDefaults.standardUserDefaults.setObject(jsonString, forKey = KEY_TOURNAMENT)
    }

    actual fun loadTournament(): String? {
        return NSUserDefaults.standardUserDefaults.stringForKey(KEY_TOURNAMENT)
    }
}
