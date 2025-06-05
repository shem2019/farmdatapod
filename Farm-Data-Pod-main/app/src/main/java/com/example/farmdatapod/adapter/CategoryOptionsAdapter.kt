package com.example.farmdatapod.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.farmdatapod.R

data class CategoryOption(val title: String, val iconResId: Int)
class CategoryOptionsAdapter(
    private val options: List<CategoryOption>,
    private val onClick: (CategoryOption) -> Unit
) : RecyclerView.Adapter<CategoryOptionsAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.icon)
        val title: TextView = view.findViewById(R.id.title)

        fun bind(option: CategoryOption) {
            icon.setImageResource(option.iconResId)
            title.text = option.title
            itemView.setOnClickListener { onClick(option) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_homepage_option, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(options[position])
    }

    override fun getItemCount() = options.size
}