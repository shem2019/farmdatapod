package com.example.farmdatapod.models

data class CIGRegistrationItem(
    val cig_name: String,
    val constitution: String,
    val date_established: String,
    val date_of_last_elections: String,
    val elections_held: String,
    val frequency: String,
    val hub: String,
    val id: Int,
    val meeting_venue: String,
    val members: List<Map<String, String>>,
    val no_of_members: Int,
    val registration: String,
    val scheduled_meeting_day: String,
    val scheduled_meeting_time: String,
    val user_id: String
)