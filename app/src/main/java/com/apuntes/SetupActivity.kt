package com.apuntes

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.apuntes.databinding.ActivitySetupBinding
import com.apuntes.ui.SeriesSetupActivity
import com.apuntes.ui.TournamentSetupActivity
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.MobileAds

class SetupActivity : AppCompatActivity() {

    private lateinit var vb: ActivitySetupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vb = ActivitySetupBinding.inflate(layoutInflater)
        setContentView(vb.root)

        // ---------------- AdMob ----------------
        fun loadAdaptiveBanner() {
            val adView = vb.adViewBanner
            val adRequest = AdRequest.Builder().build()
            val adSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, resources.displayMetrics.widthPixels)
            adView.setAdSize(adSize)
            adView.loadAd(adRequest)
        }


        // --- Navigation ---
        vb.btnStartSeries.setOnClickListener {
            startActivity(Intent(this, SeriesSetupActivity::class.java))
        }

        vb.btnOpenTournament.setOnClickListener {
            // ✅ Pass default data so TournamentSetupActivity won’t reject it
            val intent = Intent(this, TournamentSetupActivity::class.java)
            intent.putExtra("tournamentName", "Demo Tournament")
            intent.putStringArrayListExtra(
                "teamList",
                arrayListOf("Team Alpha", "Team Bravo", "Team Charlie", "Team Delta")
            )
            intent.putExtra("bestOf", 3)
            intent.putExtra("targetScore", 200)
            startActivity(intent)
        }


    }
}
