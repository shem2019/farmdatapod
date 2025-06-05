package com.example.farmdatapod.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

fun calculateDateRange(plannedPlantingDate: String, weekNumber: Int): String {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
    val calendar = Calendar.getInstance()

    // Parse the planned planting date
    calendar.time = dateFormat.parse(plannedPlantingDate) ?: Date()

    // Add (weekNumber - 1) weeks to the planting date to get the start of the selected week
    calendar.add(Calendar.WEEK_OF_YEAR, weekNumber - 1)

    // Format the start date
    val startDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

    // Add 6 days to get the end of the week
    calendar.add(Calendar.DAY_OF_YEAR, 6)

    // Format the end date
    val endDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

    // Return the date range as a string
    return "$startDate to $endDate"
}