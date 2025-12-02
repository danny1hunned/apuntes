package com.apuntes.data



data class Team(
    val name: String,
    val id: String,
    var totalScore: Int = 0,
    var gamesWon: Int = 0
)

data class ScoreRound(val teamAScore: Int, val teamBScore: Int)
