package com.example.farmdatapod.models

data class TrainingRequestModel(
    val buying_center: String,
    val content_of_training: String,
    val course_description: String,
    val course_name: String,
    val date_of_training: String,
    val participants: Participants,
    val trainer_name: String,
    val venue: String
)