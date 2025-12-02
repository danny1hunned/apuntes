package com.apuntes.shared

import kotlinx.serialization.*
import kotlinx.serialization.json.Json

@Serializable
data class StoredStanding(
    val teamName: String,
    val seriesWins: Int,
    val matchesPlayed: Int,
    val points: Int,
    val pointsAllowed: Int,
    val scoreDiff: Int
)

@Serializable
data class StoredTournament(
    val name: String,
    val timestamp: String,
    val standings: List<StoredStanding>,
    val matches: List<String> = emptyList()
)

/** Expect object to be implemented per platform */
expect object TournamentStorage {
    fun saveTournament(jsonString: String)
    fun loadTournament(): String?
}

/** Shared helper for encoding and decoding tournaments */
object TournamentSerializer {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    fun encode(tournament: StoredTournament): String = json.encodeToString(tournament)
    fun decode(jsonString: String): StoredTournament = json.decodeFromString(jsonString)
}
