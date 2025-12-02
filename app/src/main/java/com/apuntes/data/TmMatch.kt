package com.apuntes.data

import com.apuntes.tournament.model.TmTeam

/**
 * Represents a single match (series) in a tournament.
 * Shared between TournamentActivity and TournamentEngine.
 */
data class TmMatch(
    val a: TmTeam,
    val b: TmTeam,
    val bestOf: Int,
    val targetScore: Int,
    val bye: Boolean = false,
    var completed: Boolean = false,
    var winner: TmTeam? = null
)
