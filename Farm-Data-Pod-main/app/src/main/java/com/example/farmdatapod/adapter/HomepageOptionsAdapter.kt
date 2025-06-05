package com.example.farmdatapod.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.farmdatapod.R

class HomepageOptionsAdapter(
    private val options: List<HomepageOption>,
    private val onItemClick: (HomepageOption) -> Unit
) : RecyclerView.Adapter<HomepageOptionsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.icon)
        val title: TextView = view.findViewById(R.id.title)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_homepage_option, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val option = options[position]
        holder.icon.setImageResource(option.iconResId)
        holder.title.text = option.title
        holder.itemView.setOnClickListener { onItemClick(option) }
    }

    override fun getItemCount() = options.size
}

data class HomepageOption(val title: String, val iconResId: Int)