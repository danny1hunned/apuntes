package com.apuntes.ui

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.apuntes.GameActivity
import com.apuntes.databinding.ActivitySeriesSetupBinding
import com.google.android.gms.ads.AdRequest

class SeriesSetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySeriesSetupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySeriesSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ✅ Populate dropdown menus
        val bestOfOptions = listOf("1", "3", "5", "7", "9", "11")
        val targetOptions = listOf("200", "300", "500")

        val bestOfAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, bestOfOptions)
        val targetAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, targetOptions)

        binding.actvBestOf.setAdapter(bestOfAdapter)
        binding.actvTarget.setAdapter(targetAdapter)

        // ✅ Load AdMob banner
        val adRequest = AdRequest.Builder().build()
        binding.adViewBanner.loadAd(adRequest)

        // ✅ Start series button
        binding.btnStartSeries.setOnClickListener {
            val teamA = binding.etTeamA.text.toString().trim()
            val teamB = binding.etTeamB.text.toString().trim()
            val bestOf = binding.actvBestOf.text.toString().toIntOrNull() ?: 3
            val targetScore = binding.actvTarget.text.toString().toIntOrNull() ?: 200

            if (teamA.isEmpty() || teamB.isEmpty()) {
                binding.etTeamA.error = if (teamA.isEmpty()) "Required" else null
                binding.etTeamB.error = if (teamB.isEmpty()) "Required" else null
                return@setOnClickListener
            }

            val intent = Intent(this, GameActivity::class.java).apply {
                putExtra("teamAName", teamA)
                putExtra("teamBName", teamB)
                putExtra("bestOf", bestOf)
                putExtra("targetScore", targetScore)
                putExtra("tournamentMode", false)
            }
            startActivity(intent)
        }
    }
}
