package com.example.farmdatapod.dbmodels

data class User(
    val id: String,
    val email: String,
    val last_name: String,
    val other_name: String,
    val password: String,
    val role: String,
    val created_at: String,
    val updated_at: String,
    val user_type: String,
    val verification_token: String,
    val email_verified: Boolean
)