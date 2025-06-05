package com.example.farmdatapod.adapter

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class CIGProducerFieldRegAdapter(
    context: Context,
    private val producerNamesMap: Map<String, String>
) : ArrayAdapter<String>(context, android.R.layout.simple_dropdown_item_1line, producerNamesMap.values.toList()) {

    // Cache the list of producer codes and names
    private val producerCodes = producerNamesMap.keys.toList()
    private val producerNames = producerNamesMap.values.toList()

    override fun getItem(position: Int): String? {
        // Return the name at the specified position
        return producerNames.elementAtOrNull(position)
    }

    override fun getItemId(position: Int): Long {
        // Return the position index directly as the unique ID
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        // Get the view for the dropdown item
        val view = super.getView(position, convertView, parent) as TextView
        view.text = getItem(position)
        return view
    }

    // Method to get the farmer code from position
    fun getFarmerCode(position: Int): String? {
        return producerCodes.elementAtOrNull(position)
    }
}
