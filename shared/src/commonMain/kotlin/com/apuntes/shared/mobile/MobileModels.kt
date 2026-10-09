package com.apuntes.shared.mobile

import kotlinx.serialization.Serializable

@Serializable
data class MobileState(
    val schemaVersion: Int = 1,
    var revision: Long = 0,
    val matches: MutableList<MobileMatch> = mutableListOf(),
    val tournaments: MutableList<MobileTournament> = mutableListOf()
)

@Serializable
data class MobileParticipant(val id: String, val name: String, val seed: Int)

@Serializable
data class MobileScore(val id: String, var side: String, var points: Long, var reversed: Boolean = false)

@Serializable
data class MobileScoreEvent(
    val id: String,
    val kind: String,
    val side: String,
    val points: Long,
    val rowId: String,
    val createdAt: String
)

@Serializable
data class MobileGame(
    val number: Int,
    val rows: MutableList<MobileScore> = mutableListOf(),
    val events: MutableList<MobileScoreEvent> = mutableListOf(),
    var winner: String? = null
) {
    fun score(side: String): Long = rows.filter { !it.reversed && it.side == side }.sumOf { it.points }
}

@Serializable
data class MobileMatch(
    val id: String,
    val tournamentId: String? = null,
    val a: MobileParticipant,
    val b: MobileParticipant,
    val bestOf: Int,
    val target: Int,
    val kind: String = "series",
    val round: Int = 1,
    val position: Int = 1,
    var status: String = "draft",
    val games: MutableList<MobileGame> = mutableListOf(),
    var winner: String? = null,
    val createdAt: String,
    var updatedAt: String = createdAt
) {
    fun wins(side: String): Int = games.count { it.winner == side }
    fun total(side: String): Long = games.sumOf { it.score(side) }
    fun winnerId(): String? = when (winner) { "A" -> a.id; "B" -> b.id; else -> null }
    fun loserId(): String? = when (winner) { "A" -> b.id; "B" -> a.id; else -> null }
}

@Serializable
data class MobileBye(val participantId: String, val round: Int)

@Serializable
data class MobileStanding(
    val participant: MobileParticipant,
    var wins: Int = 0,
    var losses: Int = 0,
    var pointsFor: Long = 0,
    var pointsAgainst: Long = 0,
    var placement: Int? = null
)

@Serializable
data class MobileTournament(
    val id: String,
    val name: String,
    val mode: String,
    val bestOf: Int,
    val target: Int,
    val participants: List<MobileParticipant>,
    var status: String = "active",
    var round: Int = 1,
    var championId: String? = null,
    var runnerUpId: String? = null,
    var thirdId: String? = null,
    val byes: MutableList<MobileBye> = mutableListOf(),
    var standings: List<MobileStanding> = emptyList(),
    val createdAt: String,
    var updatedAt: String = createdAt
)

@Serializable
data class MobileCommand(
    val id: String,
    val revision: Long,
    val action: String,
    val timestamp: String,
    val entityId: String = "",
    val name: String = "",
    val names: List<String> = emptyList(),
    val mode: String = "roundRobin",
    val bestOf: Int = 3,
    val target: Int = 200,
    val side: String = "A",
    val points: Long = 0,
    val rowId: String = ""
)

@Serializable
data class MobileNotice(val kind: String, val winnerName: String, val scoreA: Long, val scoreB: Long)

@Serializable
data class MobileResult(
    val state: MobileState? = null,
    val error: String? = null,
    val notice: MobileNotice? = null,
    val openedId: String? = null
)
