package com.apuntes

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import com.apuntes.data.ScoreRound
import com.apuntes.data.Team
import com.apuntes.databinding.ActivityGameBinding
import com.apuntes.databinding.DialogEditScoreBinding
import com.apuntes.tournament.TournamentKeys
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.MobileAds


import kotlin.math.max

class GameActivity : AppCompatActivity() {

    private lateinit var vb: ActivityGameBinding

    // Core state
    private var teamA: Team = Team("HOME", "A")
    private var teamB: Team = Team("AWAY", "B")
    private var bestOf: Int = 3
    private var gameTargetScore: Int = 200
    private val targetWins get() = (bestOf / 2) + 1

    private val scoreHistory: MutableList<ScoreRound> = mutableListOf()

    // ✅ Track running totals across all games in the series
    private var totalSeriesPointsA = 0
    private var totalSeriesPointsB = 0

    // Tournament mode
    private var isTournament: Boolean = false
    private var tournamentMatchId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vb = ActivityGameBinding.inflate(layoutInflater)
        setContentView(vb.root)

        fun loadAdaptiveBanner() {
            val adView = vb.adViewBanner
            val adRequest = AdRequest.Builder().build()
            val adSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, resources.displayMetrics.widthPixels)
            adView.setAdSize(adSize)
            adView.loadAd(adRequest)
        }


        // Read launch extras (both normal and tournament)
        val aName = intent.getStringExtra("teamAName")
        val bName = intent.getStringExtra("teamBName")
        if (!aName.isNullOrBlank()) teamA = teamA.copy(name = aName)
        if (!bName.isNullOrBlank()) teamB = teamB.copy(name = bName)
        bestOf = intent.getIntExtra("bestOf", bestOf)
        gameTargetScore = intent.getIntExtra("targetScore", gameTargetScore)

        // Tournament extras
        isTournament = intent.getBooleanExtra(TournamentKeys.EXTRA_TOURNAMENT_MODE, false)
        tournamentMatchId = intent.getStringExtra(TournamentKeys.EXTRA_MATCH_ID)
        intent.getStringExtra(TournamentKeys.EXTRA_TEAM_A)?.let { teamA = teamA.copy(name = it) }
        intent.getStringExtra(TournamentKeys.EXTRA_TEAM_B)?.let { teamB = teamB.copy(name = it) }
        intent.getIntExtra(TournamentKeys.EXTRA_BEST_OF, bestOf).let { if (it > 0) bestOf = it }
        intent.getIntExtra(TournamentKeys.EXTRA_TARGET, gameTargetScore).let { if (it > 0) gameTargetScore = it }

        setupTopUI()
        setupRoundInput()
        setupLists()
        refreshLists()

    }

    // ---------------- AdMob ----------------


    // ---------------- UI wiring ----------------
    private fun setupTopUI() {
        vb.tvTeamATitle.text = teamA.name
        vb.tvTeamBTitle.text = teamB.name
        vb.tvBestOf.text = getString(R.string.best_of_series_short) + " " + (targetWins * 2 - 1)
        vb.btnTeamA.text = teamA.name
        vb.btnTeamB.text = teamB.name
        vb.cardCenter.setOnClickListener { /* no-op in tournament */ }
    }

    private fun setupRoundInput() {
        val submit: (String) -> Unit = { teamId ->
            val num = vb.etRoundScore.text?.toString()?.toIntOrNull()
            if (num != null) {
                submitScore(teamId, num)
                vb.etRoundScore.setText("")
            }
        }

        vb.btnTeamA.setOnClickListener { submit("A") }
        vb.btnTeamB.setOnClickListener { submit("B") }
        vb.tvUndo.setOnClickListener { undoLastScore() }
    }

    private fun setupLists() {
        val aAdapter = ScoreListAdapter(onClick = { idx -> showEditDialog(idx) })
        val bAdapter = ScoreListAdapter(onClick = { idx -> showEditDialog(idx) })

        vb.rvTeamA.layoutManager = LinearLayoutManager(this)
        vb.rvTeamB.layoutManager = LinearLayoutManager(this)
        vb.rvTeamA.adapter = aAdapter
        vb.rvTeamB.adapter = bAdapter

        LinearSnapHelper().attachToRecyclerView(vb.rvTeamA)
        LinearSnapHelper().attachToRecyclerView(vb.rvTeamB)
    }

    // ---------------- Rendering ----------------
    private fun refreshTopUI() {
        vb.tvTeamATitle.text = teamA.name
        vb.tvTeamBTitle.text = teamB.name
        vb.btnTeamA.text = teamA.name
        vb.btnTeamB.text = teamB.name

        vb.tvTeamATop.text = "${teamA.totalScore}/$gameTargetScore"
        vb.tvTeamABottom.text = "${teamA.gamesWon}/$targetWins"
        vb.tvTeamBTop.text = "${teamB.totalScore}/$gameTargetScore"
        vb.tvTeamBBottom.text = "${teamB.gamesWon}/$targetWins"

        vb.tvSeriesScore.text = "${teamA.gamesWon} - ${teamB.gamesWon}"
        vb.tvBestOf.text = getString(R.string.best_of_series_short) + " " + (targetWins * 2 - 1)
    }

    private fun refreshLists() {
        val teamAScores = scoreHistory.mapIndexedNotNull { idx, r -> if (r.teamAScore > 0) idx to r.teamAScore else null }
        val teamBScores = scoreHistory.mapIndexedNotNull { idx, r -> if (r.teamBScore > 0) idx to r.teamBScore else null }

        (vb.rvTeamA.adapter as ScoreListAdapter).submitList(teamAScores)
        (vb.rvTeamB.adapter as ScoreListAdapter).submitList(teamBScores)

        recalcTotals()
        refreshTopUI()

        if (teamAScores.isNotEmpty()) vb.rvTeamA.scrollToPosition(teamAScores.lastIndex)
        if (teamBScores.isNotEmpty()) vb.rvTeamB.scrollToPosition(teamBScores.lastIndex)
    }

    private fun recalcTotals() {
        teamA.totalScore = scoreHistory.sumOf { it.teamAScore }
        teamB.totalScore = scoreHistory.sumOf { it.teamBScore }
    }

    // ---------------- Game logic ----------------
    private fun submitScore(teamId: String, roundScore: Int) {
        scoreHistory.add(if (teamId == "A") ScoreRound(roundScore, 0) else ScoreRound(0, roundScore))
        refreshLists()
        checkForWinner()
    }

    private fun undoLastScore() {
        if (scoreHistory.isNotEmpty()) {
            scoreHistory.removeAt(scoreHistory.lastIndex)
            refreshLists()
        }
    }

    private fun deleteScore(index: Int) {
        if (index in scoreHistory.indices) {
            scoreHistory.removeAt(index)
            refreshLists()
        }
    }

    private fun editScore(index: Int, newScore: Int, teamId: String) {
        if (index in scoreHistory.indices) {
            scoreHistory[index] = if (teamId == "A") ScoreRound(newScore, 0) else ScoreRound(0, newScore)
            refreshLists()
            checkForWinner()
        }
    }

    /** Checks if a team reached the per-game target first; awards a **series game** to that team. */
    private fun checkForWinner() {
        val winnerOfGame = when {
            teamA.totalScore >= gameTargetScore && teamA.totalScore > teamB.totalScore -> teamA
            teamB.totalScore >= gameTargetScore && teamB.totalScore > teamA.totalScore -> teamB
            else -> null
        } ?: return

        // ✅ accumulate series totals BEFORE clearing
        totalSeriesPointsA += teamA.totalScore
        totalSeriesPointsB += teamB.totalScore

        val lastGameA = teamA.totalScore
        val lastGameB = teamB.totalScore

        // Award one series win
        if (winnerOfGame.id == "A") teamA.gamesWon += 1 else teamB.gamesWon += 1

        // Prepare for next game
        teamA.totalScore = 0
        teamB.totalScore = 0
        scoreHistory.clear()
        refreshLists()

        // ✅ Show dialog for this *game*
        showGameWinnerDialog(winnerOfGame.name, lastGameA, lastGameB)

        // Check if the whole series is over
        val seriesOver = max(teamA.gamesWon, teamB.gamesWon) >= targetWins
        if (!seriesOver) return

        // ---- SERIES COMPLETE ----
        if (isTournament) {
            val winner = if (teamA.gamesWon > teamB.gamesWon) teamA else teamB
            val seriesScore = "${teamA.gamesWon} - ${teamB.gamesWon}"

            // ✅ return result to TournamentActivity
            val resultIntent = Intent()
            resultIntent.putExtra("teamAName", teamA.name)
            resultIntent.putExtra("teamBName", teamB.name)
            resultIntent.putExtra("winner", winner.name)
            resultIntent.putExtra("seriesScore", seriesScore)
            setResult(RESULT_OK, resultIntent)
            finish()
            return
        }

        // ---- Regular mode (non-tournament) ----
        showWinnerDialog()
    }

    /** Show popup announcing winner of one *game* inside the series. */
    private fun showGameWinnerDialog(winnerName: String, lastA: Int, lastB: Int) {
        AlertDialog.Builder(this)
            .setTitle("Game Over")
            .setMessage(
                "Winner: $winnerName\n\n" +
                        "${teamA.name}: $lastA pts\n" +
                        "${teamB.name}: $lastB pts"
            )
            .setPositiveButton("Next Round") { d, _ -> d.dismiss() }
            .setCancelable(false)
            .show()
    }

    // ---------------- Dialogs (regular mode only) ----------------
    private fun showEditDialog(index: Int) {
        val round = scoreHistory.getOrNull(index) ?: return
        val initialScore = if (round.teamAScore > 0) round.teamAScore else round.teamBScore
        val initialA = round.teamAScore > 0

        val b = DialogEditScoreBinding.inflate(layoutInflater)
        b.etEditScore.setText(initialScore.toString())
        if (initialA) b.rbA.isChecked = true else b.rbB.isChecked = true

        AlertDialog.Builder(this)
            .setTitle("Edit Score")
            .setView(b.root)
            .setPositiveButton("Save") { _, _ ->
                val newVal = b.etEditScore.text.toString().toIntOrNull() ?: return@setPositiveButton
                val team = if (b.rbA.isChecked) "A" else "B"
                editScore(index, newVal, team)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showWinnerDialog() {
        val winner = if (teamA.gamesWon > teamB.gamesWon) teamA else teamB
        val loser = if (winner === teamA) teamB else teamA

        val seriesScore = "${winner.gamesWon} - ${loser.gamesWon}"

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.series_winner))
            .setMessage("${winner.name} won the series $seriesScore")
            .setPositiveButton("OK") { _, _ ->
                val resultIntent = Intent()
                resultIntent.putExtra("teamAName", teamA.name)
                resultIntent.putExtra("teamBName", teamB.name)
                resultIntent.putExtra("winner", winner.name)
                resultIntent.putExtra("seriesScore", seriesScore)
                setResult(RESULT_OK, resultIntent)
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun restartSeries() {
        teamA = teamA.copy(totalScore = 0, gamesWon = 0)
        teamB = teamB.copy(totalScore = 0, gamesWon = 0)
        scoreHistory.clear()
        totalSeriesPointsA = 0
        totalSeriesPointsB = 0
        refreshLists()
    }

    private fun resetToSetup() {
        finish()
    }
}
