package com.example.farmdatapod.utils

import android.app.DatePickerDialog
import android.content.Context
import android.widget.EditText
import java.text.SimpleDateFormat
import java.util.*

class DateRangePicker(private val context: Context) {
    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun show(editText: EditText) {
        showStartDatePicker(editText)
    }

    private fun showStartDatePicker(editText: EditText) {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                val startDate = dateFormat.format(calendar.time)
                showEndDatePicker(editText, startDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showEndDatePicker(editText: EditText, startDate: String) {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                val endDate = dateFormat.format(calendar.time)
                val dateRange = "$startDate to $endDate"
                editText.setText(dateRange)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
}