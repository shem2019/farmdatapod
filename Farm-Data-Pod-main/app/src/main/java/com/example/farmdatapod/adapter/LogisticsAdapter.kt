package com.example.farmdatapod.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.farmdatapod.R

data class LogisticsOption(
    val title: String,
    val iconResId: Int
)
class LogisticsAdapter(
    private val options: List<LogisticsOption>,
    private val onItemClick: (LogisticsOption) -> Unit
) : RecyclerView.Adapter<LogisticsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleTextView: TextView = view.findViewById(R.id.title)
        val iconImageView: ImageView = view.findViewById(R.id.icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_homepage_option, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val option = options[position]
        holder.titleTextView.text = option.title
        holder.iconImageView.setImageResource(option.iconResId)
        holder.itemView.setOnClickListener {
            onItemClick(option)
        }
    }

    override fun getItemCount() = options.size
}