package com.example.farmdatapod.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.farmdatapod.R

data class TrainingOption(val title: String, val iconResId: Int, val destinationId: Int)

class TrainingAdapter(
    private val trainingOptions: List<TrainingOption>,
    private val onItemClick: (Int) -> Unit
) : RecyclerView.Adapter<TrainingAdapter.TrainingViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrainingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_homepage_option, parent, false)
        return TrainingViewHolder(view)
    }

    override fun onBindViewHolder(holder: TrainingViewHolder, position: Int) {
        val trainingOption = trainingOptions[position]
        holder.bind(trainingOption, onItemClick)
    }

    override fun getItemCount(): Int = trainingOptions.size

    class TrainingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val icon: ImageView = itemView.findViewById(R.id.icon)
        private val title: TextView = itemView.findViewById(R.id.title)

        fun bind(trainingOption: TrainingOption, onItemClick: (Int) -> Unit) {
            icon.setImageResource(trainingOption.iconResId)
            title.text = trainingOption.title
            itemView.setOnClickListener { onItemClick(trainingOption.destinationId) }
        }
    }
}