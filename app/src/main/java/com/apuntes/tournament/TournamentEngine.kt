package com.apuntes.tournament

import com.apuntes.tournament.model.TmTeam
import com.apuntes.data.TmMatch

object TournamentEngine {

    /** Generate round-robin matches for a given list of teams. */
    fun generateRoundRobinGames(teams: List<TmTeam>, bestOf: Int, target: Int): List<TmMatch> {
        val matches = mutableListOf<TmMatch>()
        for (i in teams.indices) {
            for (j in i + 1 until teams.size) {
                matches += TmMatch(teams[i], teams[j], bestOf, target)
            }
        }
        return matches
    }

    /** Generate a single tie-breaker match between two tied teams. */
    fun generateTieBreaker(teamA: TmTeam, teamB: TmTeam, bestOf: Int, target: Int): List<TmMatch> {
        return listOf(TmMatch(teamA, teamB, bestOf, target))
    }
}
