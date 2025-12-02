package com.apuntes.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.apuntes.databinding.ActivityLeaderboardBinding
import com.apuntes.data.StoredTournament
import com.apuntes.data.StoredStanding
import com.apuntes.TournamentStorage
import com.apuntes.tournament.LeaderboardAdapter
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.MobileAds

class LeaderboardActivity : AppCompatActivity() {

    private lateinit var vb: ActivityLeaderboardBinding
    private lateinit var standingsAdapter: LeaderboardAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vb = ActivityLeaderboardBinding.inflate(layoutInflater)
        setContentView(vb.root)

        // ---------------- AdMob ----------------

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

        val sortedStandings: List<StoredStanding> = latest.standings.sortedWith(
            compareByDescending<StoredStanding> { it.points }
                .thenByDescending { it.seriesWins }
                .thenByDescending { it.scoreDiff }
        )

        // 🥇🥈🥉 Display top 3 names
        if (sortedStandings.isNotEmpty()) vb.tvChampionTeam.text = sortedStandings[0].teamName
        if (sortedStandings.size > 1) vb.tvSecondTeam.text = sortedStandings[1].teamName
        if (sortedStandings.size > 2) vb.tvThirdTeam.text = sortedStandings[2].teamName

        // Display all standings in the RecyclerView
        standingsAdapter.submitList(sortedStandings)
    }
}
