package com.apuntes.tournament

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.apuntes.R
import com.apuntes.data.StoredStanding

class LeaderboardAdapter :
    RecyclerView.Adapter<LeaderboardAdapter.StandingViewHolder>() {

    private var standings: List<StoredStanding> = emptyList()

    fun submitList(newList: List<StoredStanding>) {
        standings = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StandingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_leaderboard_row, parent, false)
        return StandingViewHolder(view)
    }

    override fun onBindViewHolder(holder: StandingViewHolder, position: Int) {
        val standing = standings[position]
        holder.bind(standing, position, holder.itemView.context)
    }

    override fun getItemCount(): Int = standings.size

    class StandingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val medalIcon: ImageView = itemView.findViewById(R.id.medalIcon)
        private val tvRank: TextView = itemView.findViewById(R.id.tvRank)
        private val tvTeamName: TextView = itemView.findViewById(R.id.tvTeamName)

        fun bind(standing: StoredStanding, position: Int, context: Context) {
            tvRank.text = when (position) {
                0 -> "🥇 1st"
                1 -> "🥈 2nd"
                2 -> "🥉 3rd"
                else -> "${position + 1}th"
            }

            tvTeamName.text = "${standing.teamName} - ${standing.points} pts"

            when (position) {
                0 -> medalIcon.setImageResource(R.drawable.gold_medal)
                1 -> medalIcon.setImageResource(R.drawable.silver_medal)
                2 -> medalIcon.setImageResource(R.drawable.bronze_medal)
                else -> medalIcon.setImageDrawable(null)
            }
        }
    }
}
