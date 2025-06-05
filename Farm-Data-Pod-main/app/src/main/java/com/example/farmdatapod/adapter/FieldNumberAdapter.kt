package com.example.farmdatapod.adapter

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class FieldNumberAdapter(
    context: Context,
    private val fieldNumbersMap: Map<Int, String> // Map of fieldId (Int) to fieldNumber (String)
) : ArrayAdapter<String>(context, android.R.layout.simple_dropdown_item_1line, fieldNumbersMap.values.toList()) {

    // Cache the list of field IDs and numbers
    private val fieldIds = fieldNumbersMap.keys.toList()
    private val fieldNumbers = fieldNumbersMap.values.toList()

    override fun getItem(position: Int): String? {
        // Return the field number at the specified position
        return fieldNumbers.elementAtOrNull(position)
    }

    override fun getItemId(position: Int): Long {
        // Return the field ID as the unique ID, using the position as fallback
        return fieldIds.elementAtOrNull(position)?.toLong() ?: position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        // Get the view for the dropdown item and set the text to the field number
        val view = super.getView(position, convertView, parent) as TextView
        view.text = getItem(position)
        return view
    }

    // Method to get the field number by position
    fun getFieldNumber(position: Int): String? {
        // Return the field number at the specified position
        return fieldNumbers.elementAtOrNull(position)
    }

    // Method to get the field ID from the given position
    fun getFieldId(position: Int): Int? {
        // Return the field ID at the specified position
        return fieldIds.elementAtOrNull(position)
    }
}