// FILE: TournamentKeys.kt
package com.apuntes.tournament

/**
 * Keys used to pass data between TournamentActivity and GameActivity
 * and to return results back to TournamentActivity.
 */
object TournamentKeys {

    // ----------- EXTRAS (GameActivity launched in tournament mode) -----------
    /** Boolean flag: true if GameActivity was launched from a tournament */
    const val EXTRA_TOURNAMENT_MODE = "EXTRA_TOURNAMENT_MODE"

    /** Unique id for the tournament match being played */
    const val EXTRA_MATCH_ID = "EXTRA_MATCH_ID"

    /** Team A name passed to GameActivity */
    const val EXTRA_TEAM_A = "EXTRA_TEAM_A"

    /** Team B name passed to GameActivity */
    const val EXTRA_TEAM_B = "EXTRA_TEAM_B"

    /** Best-of value for this match (e.g. 3, 5, 7) */
    const val EXTRA_BEST_OF = "EXTRA_BEST_OF"

    /** Target score per game (e.g. 200 etc.) */
    const val EXTRA_TARGET = "EXTRA_TARGET"


    // ----------- RESULTS (returned from GameActivity on match end) -----------
    /** Match ID returned after GameActivity finishes */
    const val RES_MATCH_ID = "RES_MATCH_ID"

    /** Winner team name returned to TournamentActivity */
    const val RES_WINNER = "RES_WINNER"

    /** Loser team name returned to TournamentActivity */
    const val RES_LOSER = "RES_LOSER"

    /** How many wins the winner got in the series */
    const val RES_WIN_GAMES = "RES_WIN_GAMES"

    /** How many wins the loser got in the series */
    const val RES_LOSE_GAMES = "RES_LOSE_GAMES"

    /** Total points scored by Team A during the full series */
    const val RES_TEAM_A_POINTS = "RES_TEAM_A_POINTS"

    /** Total points scored by Team B during the full series */
    const val RES_TEAM_B_POINTS = "RES_TEAM_B_POINTS"

    //  total points across a full series (Best Of)
    const val RES_TOTAL_A_POINTS = "res_total_a_points"
    const val RES_TOTAL_B_POINTS = "res_total_b_points"



}
