package com.apuntes

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.apuntes.databinding.ItemScoreBinding

class ScoreListAdapter(
    private val onClick: (index: Int) -> Unit
) : ListAdapter<Pair<Int, Int>, ScoreListAdapter.VH>(DIFF) {

    object DIFF : DiffUtil.ItemCallback<Pair<Int, Int>>() {
        override fun areItemsTheSame(o: Pair<Int, Int>, n: Pair<Int, Int>) = o.first == n.first
        override fun areContentsTheSame(o: Pair<Int, Int>, n: Pair<Int, Int>) = o == n
    }

    inner class VH(val vb: ItemScoreBinding) : RecyclerView.ViewHolder(vb.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val vb = ItemScoreBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(vb)
    }

    override fun onBindViewHolder(h: VH, position: Int) {
        val (originalIndex, score) = getItem(position)
        h.vb.tvScore.text = score.toString()
        h.vb.root.setOnClickListener { onClick(originalIndex) }
    }
}