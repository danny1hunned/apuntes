package com.apuntes.ui

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.apuntes.GameActivity
import com.apuntes.shared.TournamentStorage
import com.apuntes.data.StoredStanding
import com.apuntes.data.StoredTournament
import com.apuntes.databinding.ActivityTournamentBinding
import com.apuntes.tournament.LeaderboardActivity
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class TournamentActivity : AppCompatActivity() {

    private lateinit var vb: ActivityTournamentBinding
    private lateinit var matchLauncher: ActivityResultLauncher<Intent>

    // Ads
    private var interstitialAd: InterstitialAd? = null

    private val matches = mutableListOf<Pair<String, String>>() // current round matches
    private val matchStatus = mutableMapOf<Pair<String, String>, String>()
    private val teamWins = mutableMapOf<String, Int>()
    private var advancingTeams = mutableListOf<String>()

    private var currentPhase = "ROUND_ROBIN"
    private var tournamentMode = "Round Robin"
    private var bestOf = 3
    private var targetScore = 200
    private var champion: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vb = ActivityTournamentBinding.inflate(layoutInflater)
        setContentView(vb.root)

        // Toolbar
        vb.toolbar.setNavigationOnClickListener { finish() }

        // Banner Ad
        loadAdaptiveBanner()

        // Interstitial Ad
        loadInterstitialAd()

        // Retrieve tournament settings
        val teamNames = intent.getStringArrayListExtra("teamList") ?: arrayListOf()
        val tournamentName = intent.getStringExtra("tournamentName") ?: "Tournament"
        bestOf = intent.getIntExtra("bestOf", 3)
        targetScore = intent.getIntExtra("targetScore", 200)
        tournamentMode = intent.getStringExtra("mode") ?: "Round Robin"

        vb.tvTournamentName.text = tournamentName
        vb.rvMatches.layoutManager = LinearLayoutManager(this)

        teamNames.forEach { teamWins[it] = 0 }
        advancingTeams.addAll(teamNames)

        // Handle match results
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
                    teamWins[winner] = (teamWins[winner] ?: 0) + 1
                    vb.rvMatches.adapter?.notifyDataSetChanged()

                    // Show ad AFTER match
                    showInterstitialAd()

                    if (tournamentMode == "Elimination") {
                        handleEliminationProgress(teamA, teamB, winner)
                    } else {
                        handleRoundRobinProgress()
                    }
                }
            }
        }

        // Start correct mode
        if (tournamentMode == "Elimination") {
            startEliminationRound()
        } else {
            startRoundRobin(teamNames)
        }
    }

    // =============================
    // ===  ROUND ROBIN MODE    ===
    // =============================
    private fun startRoundRobin(teamNames: List<String>) {
        currentPhase = "ROUND_ROBIN"
        matches.clear()
        for (i in 0 until teamNames.size)
            for (j in i + 1 until teamNames.size)
                matches.add(teamNames[i] to teamNames[j])

        vb.tvMatchCount.text = "${matches.size} matches to play"
        setupAdapter()
    }

    private fun handleRoundRobinProgress() {
        if (currentPhase == "ROUND_ROBIN" && matchStatus.size == matches.size) {
            currentPhase = "FINALS"
            startFinalMatch()
        }
    }

    private fun startFinalMatch() {
        val topTwo = teamWins.entries.sortedByDescending { it.value }.take(2)
        if (topTwo.size < 2) {
            showChampionDialog()
            return
        }

        val team1 = topTwo[0].key
        val team2 = topTwo[1].key

        if (champion != null) return

        AlertDialog.Builder(this)
            .setTitle("🏁 Tournament Final")
            .setMessage("Finalist teams:\n\n$team1 vs $team2\n\nStart the final match?")
            .setCancelable(false)
            .setPositiveButton("Start") { _, _ ->
                launchFinalMatch(team1, team2)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // =============================
    // ===  ELIMINATION MODE    ===
    // =============================
    private fun startEliminationRound() {
        currentPhase = "ELIMINATION"
        matches.clear()

        val localTeams = advancingTeams.toMutableList()
        localTeams.shuffle()

        for (i in 0 until localTeams.size step 2) {
            if (i + 1 < localTeams.size)
                matches.add(localTeams[i] to localTeams[i + 1])
            else {
                teamWins[localTeams[i]] = (teamWins[localTeams[i]] ?: 0) + 1
                advancingTeams.add(localTeams[i])
            }
        }

        vb.tvMatchCount.text = "Current round: ${matches.size} matches"
        setupAdapter()
    }

    private fun handleEliminationProgress(teamA: String, teamB: String, winner: String) {
        advancingTeams.add(winner)
        val allPlayed = matches.all { matchStatus.containsKey(it) }

        if (!allPlayed) return

        val nextRoundTeams = advancingTeams.toMutableList()
        advancingTeams.clear()

        if (nextRoundTeams.size == 1) {
            if (champion == null) {
                champion = nextRoundTeams.first()
                advancingTeams.clear()
                showChampionDialog()
            }
            return
        }

        AlertDialog.Builder(this)
            .setTitle("🏁 Next Round")
            .setMessage("${nextRoundTeams.size} teams advance. Start the next round?")
            .setCancelable(false)
            .setPositiveButton("Start") { _, _ ->
                matches.clear()
                for (i in 0 until nextRoundTeams.size step 2) {
                    if (i + 1 < nextRoundTeams.size)
                        matches.add(nextRoundTeams[i] to nextRoundTeams[i + 1])
                    else advancingTeams.add(nextRoundTeams[i])
                }
                matchStatus.clear()
                vb.tvMatchCount.text = "Next round: ${matches.size} matches"
                setupAdapter()
            }
            .show()
    }

    // =============================
    // ===  SHARED HELPERS       ===
    // =============================
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

    private fun showChampionDialog() {
        val winner = champion ?: teamWins.maxByOrNull { it.value }?.key ?: "Unknown"

        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        val standings = teamWins.entries.map { (team, wins) ->
            StoredStanding(
                teamName = team,
                seriesWins = wins,
                matchesPlayed = wins,
                points = wins * 3,
                pointsAllowed = 0,
                scoreDiff = 0
            )
        }

        val storedTournament = StoredTournament(
            name = if (tournamentMode == "Elimination") "Elimination Tournament" else "Round Robin Tournament",
            timestamp = timestamp,
            standings = standings,
            matches = emptyList()
        )
        TournamentStorage.saveTournament(this, storedTournament)

        AlertDialog.Builder(this)
            .setTitle("🏆 Champion Declared!")
            .setMessage("Champion: $winner\n\nWould you like to view the leaderboard?")
            .setCancelable(false)
            .setPositiveButton("View Leaderboard") { _, _ ->
                val intent = Intent(this, LeaderboardActivity::class.java)
                intent.putExtra("champion", winner)
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Close") { _, _ ->
                finish()
            }
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

    // =============================
    // ===  ADS UTILITY METHODS ===
    // =============================
    private fun loadInterstitialAd() {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            this,
            "ca-app-pub-8197142441567841/2137080929", //  interstitial ID
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                }
            }
        )
    }

    private fun showInterstitialAd() {
        interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                loadInterstitialAd()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                interstitialAd = null
            }
        }

        interstitialAd?.show(this)
    }

    private fun loadAdaptiveBanner() {
        val adView = vb.adViewBanner
        val adRequest = AdRequest.Builder().build()
        val adSize = AdSize.BANNER
        adView.setAdSize(adSize)
        adView.loadAd(adRequest)
    }
}
