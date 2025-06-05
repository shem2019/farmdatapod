package com.example.farmdatapod.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.farmdatapod.R

data class CustomerType(val icon: Int, val title: String)

class CustomersAdapter(
    private val customerTypes: List<CustomerType>,
    private val onItemClick: (CustomerType) -> Unit
) : RecyclerView.Adapter<CustomersAdapter.ViewHolder>() {

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
        val customerType = customerTypes[position]
        holder.icon.setImageResource(customerType.icon)
        holder.title.text = customerType.title
        holder.itemView.setOnClickListener { onItemClick(customerType) }
    }

    override fun getItemCount() = customerTypes.size
}