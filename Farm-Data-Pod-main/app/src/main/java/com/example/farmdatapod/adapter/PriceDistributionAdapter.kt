package com.example.farmdatapod.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.farmdatapod.R

class PriceDistributionAdapter(private val onItemClick: (String) -> Unit) :
    RecyclerView.Adapter<PriceDistributionAdapter.ViewHolder>() {

    private val options = listOf("Farmer", "Customer")

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.icon)
        val title: TextView = itemView.findViewById(R.id.title)

        init {
            itemView.setOnClickListener {
                onItemClick(options[adapterPosition])
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_homepage_option, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val option = options[position]
        holder.title.text = option
        holder.icon.setImageResource(
            when (option) {
                "Farmer" -> R.drawable.hub // Replace with actual farmer icon
                "Customer" -> R.drawable.price_distribution // Replace with actual customer icon
                else -> R.drawable.logo
            }
        )
    }

    override fun getItemCount() = options.size
}