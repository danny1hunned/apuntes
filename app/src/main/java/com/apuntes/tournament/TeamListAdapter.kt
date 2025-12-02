package com.apuntes.tournament

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.apuntes.R
import com.apuntes.tournament.model.TmTeam

class TeamListAdapter(
    private val teams: MutableList<TmTeam>,
    private val onTeamRemoved: ((TmTeam) -> Unit)? = null
) : RecyclerView.Adapter<TeamListAdapter.TeamViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TeamViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_team_row, parent, false)
        return TeamViewHolder(view)
    }

    override fun getItemCount(): Int = teams.size

    override fun onBindViewHolder(holder: TeamViewHolder, position: Int) {
        val team = teams[position]
        holder.tvTeamName.text = team.name

        holder.btnRemove.setOnClickListener {
            val removed = teams.removeAt(position)
            notifyItemRemoved(position)
            onTeamRemoved?.invoke(removed)
        }
    }

    class TeamViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTeamName: TextView = view.findViewById(R.id.tvTeamName)
        val btnRemove: ImageButton = view.findViewById(R.id.btnRemoveTeam)
    }
}
