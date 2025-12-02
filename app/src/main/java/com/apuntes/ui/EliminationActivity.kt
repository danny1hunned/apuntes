package com.apuntes.ui

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.apuntes.GameActivity
import com.apuntes.TournamentStorage
import com.apuntes.data.StoredStanding
import com.apuntes.data.StoredTournament
import com.apuntes.databinding.ActivityTournamentBinding
import com.apuntes.tournament.LeaderboardActivity
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class EliminationActivity : AppCompatActivity() {

    private lateinit var vb: ActivityTournamentBinding
    private lateinit var matchLauncher: ActivityResultLauncher<Intent>

    private val matches = mutableListOf<Pair<String, String>>() // current matches
    private val matchStatus = mutableMapOf<Pair<String, String>, String>()
    private val winnersThisRound = mutableListOf<String>()
    private val eliminatedTeams = mutableListOf<String>()

    private var targetScore = 200
    private var roundNumber = 1
    private var champion: String? = null
    private var thirdPlace: String? = null

    // 3-team elimination tracking
    private var threeTeamMode = false
    private var roundStep = 1
    private var teamA: String? = null
    private var teamB: String? = null
    private var teamC: String? = null
    private var firstWinner: String? = null
    private var firstLoser: String? = null
    private var secondWinner: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vb = ActivityTournamentBinding.inflate(layoutInflater)
        setContentView(vb.root)

        vb.toolbar.setNavigationOnClickListener { finish() }

        val teamNames = intent.getStringArrayListExtra("teamList") ?: arrayListOf()
        val tournamentName = intent.getStringExtra("tournamentName") ?: "Elimination Tournament"
        targetScore = intent.getIntExtra("targetScore", 200)

        vb.tvTournamentName.text = tournamentName
        vb.rvMatches.layoutManager = LinearLayoutManager(this)

        setupMatchLauncher()

        if (teamNames.size == 3) {
            threeTeamMode = true
            setupThreeTeamElimination(teamNames)
        } else {
            startStandardElimination(teamNames)
        }
    }

    // =========================================================
    // ============ NORMAL ELIMINATION (4+ TEAMS) ==============
    // =========================================================
    private fun startStandardElimination(teams: List<String>) {
        vb.tvMatchCount.text = "Round $roundNumber"
        matches.clear()
        matchStatus.clear()
        winnersThisRound.clear()

        for (i in 0 until teams.size step 2) {
            if (i + 1 < teams.size)
                matches.add(teams[i] to teams[i + 1])
        }

        setupAdapter()
    }

    private fun handleStandardRoundCompletion() {
        if (matchStatus.size < matches.size) return

        when {
            winnersThisRound.size == 1 -> {
                champion = winnersThisRound.first()
                showChampionDialog()
            }

            winnersThisRound.size == 2 -> {
                // Semifinal → Final
                AlertDialog.Builder(this)
                    .setTitle("🏁 Final Round")
                    .setMessage("${winnersThisRound[0]} vs ${winnersThisRound[1]}")
                    .setPositiveButton("Start") { _, _ ->
                        matches.clear()
                        matchStatus.clear()
                        matches.add(winnersThisRound[0] to winnersThisRound[1])
                        setupAdapter(isFinal = true)
                    }
                    .setCancelable(false)
                    .show()
            }

            else -> {
                roundNumber++
                AlertDialog.Builder(this)
                    .setTitle("Next Round")
                    .setMessage("Round $roundNumber ready.\n${winnersThisRound.size} teams advance.")
                    .setPositiveButton("Start") { _, _ ->
                        startStandardElimination(winnersThisRound.toList())
                    }
                    .setCancelable(false)
                    .show()
            }
        }
    }

    // =========================================================
    // ============== 3-TEAM ELIMINATION MODE ==================
    // =========================================================
    private fun setupThreeTeamElimination(teams: List<String>) {
        vb.tvMatchCount.text = "3-Team Tournament"
        teamA = teams[0]
        teamB = teams[1]
        teamC = teams[2]

        matches.clear()
        matchStatus.clear()
        matches.add(teamA!! to teamB!!) // Step 1: A vs B
        setupAdapter()
    }

    private fun handleThreeTeamProgress(winner: String, loser: String) {
        when (roundStep) {
            1 -> {
                // After Match 1 (A vs B)
                firstWinner = winner
                firstLoser = loser
                roundStep = 2

                AlertDialog.Builder(this)
                    .setTitle("Next Match")
                    .setMessage("$teamC vs $firstLoser\nLoser is eliminated!")
                    .setPositiveButton("Start") { _, _ ->
                        matches.clear()
                        matchStatus.clear()
                        matches.add(teamC!! to firstLoser!!)
                        setupAdapter()
                    }
                    .setCancelable(false)
                    .show()
            }

            2 -> {
                // After Match 2 (C vs Loser of Match 1)
                secondWinner = winner
                thirdPlace = loser
                roundStep = 3

                AlertDialog.Builder(this)
                    .setTitle("🏆 Championship Match")
                    .setMessage("$firstWinner vs $secondWinner\nWinner becomes champion!")
                    .setPositiveButton("Start") { _, _ ->
                        matches.clear()
                        matchStatus.clear()
                        matches.add(firstWinner!! to secondWinner!!)
                        setupAdapter(isFinal = true)
                    }
                    .setCancelable(false)
                    .show()
            }

            3 -> {
                // Final played → declare champion
                champion = winner
                showChampionDialog()
            }
        }
    }

    // =========================================================
    // =============== GAME HANDLING / ADAPTER =================
    // =========================================================
    private fun setupMatchLauncher() {
        matchLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val data = result.data
                val teamA = data?.getStringExtra("teamAName")
                val teamB = data?.getStringExtra("teamBName")
                val winner = data?.getStringExtra("winner")

                if (teamA != null && teamB != null && winner != null) {
                    matchStatus[teamA to teamB] = "✅ $winner won"
                    vb.rvMatches.adapter?.notifyDataSetChanged()

                    val loser = if (winner == teamA) teamB else teamA
                    winnersThisRound.add(winner)

                    if (threeTeamMode)
                        handleThreeTeamProgress(winner, loser)
                    else
                        handleStandardRoundCompletion()
                }
            }
        }
    }

    private fun setupAdapter(isFinal: Boolean = false) {
        val adapter = MatchAdapter(
            matches,
            getStatus = { matchStatus[it] ?: "⚪ Not played" }
        ) { match ->
            val intent = Intent(this, GameActivity::class.java)
            intent.putExtra("teamAName", match.first)
            intent.putExtra("teamBName", match.second)
            intent.putExtra("bestOf", 1) // Always one match in elimination
            intent.putExtra("targetScore", targetScore)
            intent.putExtra("tournamentMode", true)
            matchLauncher.launch(intent)
        }
        vb.rvMatches.adapter = adapter
    }

    // =========================================================
    // ==================== CHAMPION ===========================
    // =========================================================
    private fun showChampionDialog() {
        val winner = champion ?: "Unknown"
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))

        val standings = mutableListOf<StoredStanding>()
        standings.add(StoredStanding(winner, 1, 1, 10, 0, 0))
        thirdPlace?.let {
            standings.add(StoredStanding(it, 0, 1, 7, 0, 0))
        }

        val storedTournament = StoredTournament(
            name = "Elimination Tournament",
            timestamp = timestamp,
            standings = standings,
            matches = emptyList()
        )
        TournamentStorage.saveTournament(this, storedTournament)

        AlertDialog.Builder(this)
            .setTitle("🏆 Champion Declared!")
            .setMessage("Champion: $winner\n3rd Place: ${thirdPlace ?: "N/A"}")
            .setCancelable(false)
            .setPositiveButton("View Leaderboard") { _, _ ->
                val intent = Intent(this, LeaderboardActivity::class.java)
                intent.putExtra("champion", winner)
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Close") { _, _ -> finish() }
            .show()
    }
}
