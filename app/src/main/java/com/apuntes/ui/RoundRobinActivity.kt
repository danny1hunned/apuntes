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

class RoundRobinActivity : AppCompatActivity() {

    private lateinit var vb: ActivityTournamentBinding
    private lateinit var matchLauncher: ActivityResultLauncher<Intent>

    private val matches = mutableListOf<Pair<String, String>>()
    private val matchStatus = mutableMapOf<Pair<String, String>, String>()
    private val teamWins = mutableMapOf<String, Int>()

    private var bestOf = 3
    private var targetScore = 200
    private var champion: String? = null
    private var tournamentName = "Tournament"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vb = ActivityTournamentBinding.inflate(layoutInflater)
        setContentView(vb.root)

        vb.toolbar.setNavigationOnClickListener { finish() }

        val teamNames = intent.getStringArrayListExtra("teamList") ?: arrayListOf()
        tournamentName = intent.getStringExtra("tournamentName") ?: "Tournament"
        bestOf = intent.getIntExtra("bestOf", 3)
        targetScore = intent.getIntExtra("targetScore", 200)

        vb.tvTournamentName.text = tournamentName
        vb.rvMatches.layoutManager = LinearLayoutManager(this)

        // Initialize win tracker
        teamNames.forEach { teamWins[it] = 0 }

        // Build round-robin matchups
        for (i in 0 until teamNames.size)
            for (j in i + 1 until teamNames.size)
                matches.add(teamNames[i] to teamNames[j])

        vb.tvMatchCount.text = "${matches.size} matches to play"
        setupAdapter()

        // Handle results from GameActivity
        matchLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val data = result.data
                val teamA = data?.getStringExtra("teamAName")
                val teamB = data?.getStringExtra("teamBName")
                val winner = data?.getStringExtra("winner")

                if (teamA != null && teamB != null && winner != null) {
                    matchStatus[teamA to teamB] = "✅ $winner won"
                    teamWins[winner] = (teamWins[winner] ?: 0) + 1
                    vb.rvMatches.adapter?.notifyDataSetChanged()

                    if (matchStatus.size == matches.size) startFinalMatch()
                }
            }
        }
    }

    private fun setupAdapter() {
        val adapter = MatchAdapter(
            matches,
            getStatus = { matchStatus[it] ?: "⚪ Not played" }
        ) { match ->
            launchMatch(match.first, match.second)
        }
        vb.rvMatches.adapter = adapter
    }

    private fun launchMatch(teamA: String, teamB: String) {
        val intent = Intent(this, GameActivity::class.java)
        intent.putExtra("teamAName", teamA)
        intent.putExtra("teamBName", teamB)
        intent.putExtra("bestOf", bestOf)
        intent.putExtra("targetScore", targetScore)
        intent.putExtra("tournamentMode", true)
        matchLauncher.launch(intent)
    }

    private fun startFinalMatch() {
        val topTwo = teamWins.entries.sortedByDescending { it.value }.take(2)
        if (topTwo.size < 2) {
            showChampionDialog()
            return
        }

        val team1 = topTwo[0].key
        val team2 = topTwo[1].key

        AlertDialog.Builder(this)
            .setTitle("🏁 Championship Match")
            .setMessage("Final teams:\n$team1 vs $team2\n\nStart the final?")
            .setPositiveButton("Start") { _, _ ->
                launchFinalMatch(team1, team2)
            }
            .setCancelable(false)
            .show()
    }

    private fun launchFinalMatch(team1: String, team2: String) {
        val intent = Intent(this, GameActivity::class.java)
        intent.putExtra("teamAName", team1)
        intent.putExtra("teamBName", team2)
        intent.putExtra("bestOf", bestOf)
        intent.putExtra("targetScore", targetScore)
        intent.putExtra("tournamentMode", true)
        matchLauncher.launch(intent)
    }

    private fun showChampionDialog() {
        val winner = champion ?: teamWins.maxByOrNull { it.value }?.key ?: "Unknown"

        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        val standings = teamWins.entries.map { (team, wins) ->
            StoredStanding(team, wins, wins, wins * 3, 0, 0)
        }

        val storedTournament = StoredTournament(
            name = "$tournamentName (Round Robin)",
            timestamp = timestamp,
            standings = standings,
            matches = emptyList()
        )
        TournamentStorage.saveTournament(this, storedTournament)

        AlertDialog.Builder(this)
            .setTitle("🏆 Champion Declared!")
            .setMessage("Champion: $winner\n\nView leaderboard?")
            .setPositiveButton("View") { _, _ ->
                val intent = Intent(this, LeaderboardActivity::class.java)
                intent.putExtra("champion", winner)
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Close") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }
}
