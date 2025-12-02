package com.apuntes.ui

import android.app.AlertDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.apuntes.databinding.ActivityTournamentSetupBinding
import android.content.Intent
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize

class TournamentSetupActivity : AppCompatActivity() {

    private lateinit var vb: ActivityTournamentSetupBinding
    private val teamList = mutableListOf<String>()
    private lateinit var teamAdapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vb = ActivityTournamentSetupBinding.inflate(layoutInflater)
        setContentView(vb.root)

        fun loadAdaptiveBanner() {
            val adView = vb.adViewBanner
            val adRequest = AdRequest.Builder().build()
            val adSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, resources.displayMetrics.widthPixels)
            adView.setAdSize(adSize)
            adView.loadAd(adRequest)
        }

        // Initialize List Adapter
        teamAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, teamList)
        vb.listTeams.adapter = teamAdapter

        setupDropdowns()
        setupTeamInput()
        setupStartButton()
    }

    private fun setupDropdowns() {
        // Best Of Dropdown (odd numbers from 1 to infinity)
        val bestOfNumbers = generateSequence(1) { it + 2 }.take(100).toList()
        val bestOfAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, bestOfNumbers)
        vb.dropdownBestOf.setAdapter(bestOfAdapter)
        vb.dropdownBestOf.setText("3", false)

        // Tournament Mode Dropdown
        val modes = listOf("Round Robin", "Elimination")
        val modeAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, modes)
        vb.dropdownMode.setAdapter(modeAdapter)
        vb.dropdownMode.setText("Round Robin", false)
    }

    private fun setupTeamInput() {
        vb.btnAddTeam.setOnClickListener {
            val teamName = vb.etTeamInput.text.toString().trim()
            if (teamName.isEmpty()) {
                Toast.makeText(this, "Enter a team name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (teamList.contains(teamName)) {
                Toast.makeText(this, "Team already added", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            teamList.add(teamName)
            teamAdapter.notifyDataSetChanged()
            vb.etTeamInput.text?.clear()
        }

        vb.listTeams.setOnItemClickListener { _, _, position, _ ->
            val teamName = teamList[position]
            showTeamOptionsDialog(teamName, position)
        }
    }

    private fun showTeamOptionsDialog(teamName: String, position: Int) {
        val options = arrayOf("Rename", "Delete")
        AlertDialog.Builder(this)
            .setTitle("Manage Team")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showRenameDialog(teamName, position)
                    1 -> {
                        teamList.removeAt(position)
                        teamAdapter.notifyDataSetChanged()
                    }
                }
            }
            .show()
    }

    private fun showRenameDialog(oldName: String, position: Int) {
        val input = EditText(this)
        input.setText(oldName)

        AlertDialog.Builder(this)
            .setTitle("Rename Team")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isEmpty()) {
                    Toast.makeText(this, "Team name cannot be empty", Toast.LENGTH_SHORT).show()
                } else {
                    teamList[position] = newName
                    teamAdapter.notifyDataSetChanged()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupStartButton() {
        vb.btnStartTournament.setOnClickListener {
            val tournamentName = vb.etTournamentName.text.toString().trim()
            val bestOf = vb.dropdownBestOf.text.toString().toIntOrNull() ?: 3
            val targetScore = vb.etTargetScore.text.toString().toIntOrNull() ?: 200
            val mode = vb.dropdownMode.text.toString()

            if (tournamentName.isEmpty()) {
                Toast.makeText(this, "Enter a tournament name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (teamList.size < 2) {
                Toast.makeText(this, "At least 2 teams required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = when (mode) {
                "Elimination" -> Intent(this, EliminationActivity::class.java)
                else -> Intent(this, RoundRobinActivity::class.java)
            }

            intent.putStringArrayListExtra("teamList", ArrayList(teamList))
            intent.putExtra("tournamentName", tournamentName)
            intent.putExtra("bestOf", bestOf)
            intent.putExtra("targetScore", targetScore)
            intent.putExtra("mode", mode)

            startActivity(intent)
        }
    }
}
