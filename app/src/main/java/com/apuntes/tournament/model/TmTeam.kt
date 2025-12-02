package com.apuntes.tournament.model

/**
 * Represents a single team in a tournament.
 * Shared between TournamentActivity and TournamentEngine.
 *
 * Fields track:
 *  - seriesWins: how many match series the team has won
 *  - pointsAllowed: total points conceded to opponents
 *  - scoreDiff: total point differential (pointsFor - pointsAgainst)
 *  - byeCount: number of times advanced automatically (BYE)
 */
data class TmTeam(
    val name: String,
    var seriesWins: Int = 0,
    var pointsAllowed: Int = 0,
    var scoreDiff: Int = 0,
    var byeCount: Int = 0
) {

    companion object {
        /** Shared constant for "no opponent" placeholder (used for BYE). */
        val BYE = TmTeam("_BYE_")
    }


}

