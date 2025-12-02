package com.apuntes.tournament

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.apuntes.databinding.ActivityLeaderboardBinding
import com.apuntes.data.StoredTournament
import com.apuntes.data.StoredStanding
import com.apuntes.TournamentStorage
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize

class LeaderboardActivity : AppCompatActivity() {

    private lateinit var vb: ActivityLeaderboardBinding
    private lateinit var standingsAdapter: LeaderboardAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vb = ActivityLeaderboardBinding.inflate(layoutInflater)
        setContentView(vb.root)

        fun loadAdaptiveBanner() {
            val adView = vb.adViewBanner
            val adRequest = AdRequest.Builder().build()
            val adSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, resources.displayMetrics.widthPixels)
            adView.setAdSize(adSize)
            adView.loadAd(adRequest)
        }

        standingsAdapter = LeaderboardAdapter()
        vb.rvStandings.layoutManager = LinearLayoutManager(this)
        vb.rvStandings.adapter = standingsAdapter

        loadAndDisplayLeaderboard()
    }

    private fun loadAndDisplayLeaderboard() {
        val latest: StoredTournament? = TournamentStorage.loadLatestTournament(this)
        if (latest == null) {
            vb.tvTitle.text = "No tournaments found"
            return
        }

        vb.tvTitle.text = "🏆 ${latest.name} — ${latest.timestamp}"

        // ✅ Sort teams: Wins DESC → ScoreDiff DESC → PointsAllowed ASC
        val sortedStandings: List<StoredStanding> = latest.standings.sortedWith(
            compareByDescending<StoredStanding> { it.seriesWins }
                .thenByDescending { it.scoreDiff }
                .thenBy { it.pointsAllowed }
        )

        standingsAdapter.submitList(sortedStandings)
    }
}
