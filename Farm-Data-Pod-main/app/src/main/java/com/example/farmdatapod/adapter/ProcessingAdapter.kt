package com.example.farmdatapod.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.farmdatapod.R

data class ProcessingPlant(val name: String, val iconResId: Int)

class ProcessingAdapter(
    private val plants: List<ProcessingPlant>,
    private val onItemClick: (ProcessingPlant) -> Unit
) : RecyclerView.Adapter<ProcessingAdapter.ProcessingViewHolder>() {

    inner class ProcessingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.icon)
        val title: TextView = view.findViewById(R.id.title)

        fun bind(plant: ProcessingPlant) {
            icon.setImageResource(plant.iconResId)
            title.text = plant.name
            itemView.setOnClickListener {
                onItemClick(plant)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProcessingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_homepage_option, parent, false)
        return ProcessingViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProcessingViewHolder, position: Int) {
        holder.bind(plants[position])
    }

    override fun getItemCount() = plants.size
}