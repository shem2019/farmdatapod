package com.example.farmdatapod.adapter

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class ProducerBiodataRegAdapter(
    context: Context,
    private val producerNamesMap: Map<Int, String> // Accepts Int as key
) : ArrayAdapter<String>(context, android.R.layout.simple_dropdown_item_1line, producerNamesMap.values.toList()) {

    // Cache the list of producer IDs and names
    private val producerIds = producerNamesMap.keys.toList() // IDs as Int
    private val producerNames = producerNamesMap.values.toList()

    override fun getItem(position: Int): String? {
        // Return the name at the specified position
        return producerNames.elementAtOrNull(position)
    }

    override fun getItemId(position: Int): Long {
        // Return the ID of the producer as the unique ID, using position if null
        return producerIds.elementAtOrNull(position)?.toLong() ?: position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        // Get the view for the dropdown item
        val view = super.getView(position, convertView, parent) as TextView
        view.text = getItem(position)
        return view
    }

    // Method to get the producer ID from the given position
    fun getProducerId(position: Int): Int? {
        return producerIds.elementAtOrNull(position)
    }
}
