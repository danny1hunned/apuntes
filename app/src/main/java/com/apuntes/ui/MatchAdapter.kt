package com.apuntes.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.apuntes.databinding.ItemMatchCardBinding

class MatchAdapter(
    private val matches: List<Pair<String, String>>,
    private val getStatus: (Pair<String, String>) -> String,
    private val onMatchClick: (Pair<String, String>) -> Unit
) : RecyclerView.Adapter<MatchAdapter.MatchViewHolder>() {

    inner class MatchViewHolder(val binding: ItemMatchCardBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MatchViewHolder {
        val binding = ItemMatchCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MatchViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MatchViewHolder, position: Int) {
        val match = matches[position]
        val (teamA, teamB) = match
        holder.binding.tvMatchLabel.text = "Match ${position + 1}"
        holder.binding.tvMatchTeams.text = "$teamA vs $teamB"
        holder.binding.tvMatchStatus.text = getStatus(match)
        holder.itemView.setOnClickListener { onMatchClick(match) }
    }

    override fun getItemCount() = matches.size
}
