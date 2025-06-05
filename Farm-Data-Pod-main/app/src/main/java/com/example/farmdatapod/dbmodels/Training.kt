package com.example.farmdatapod

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Date

class Training {
    var id: Int = 0
    var course_name: String? = null
    var trainer_name: String? = null
    var buying_center: String? = null
    var course_description: String? = null
    var date_of_training: String? = null // Keep as String to match the format
    var content_of_training: String? = null
    var venue: String? = null
    var participants: Map<String, String>? = null
    var user_id: String? = null // Adjusted to String to match JSON format

    // Helper methods to handle participants JSON
    fun getParticipantsJson(): String? {
        return participants?.let {
            Gson().toJson(it)
        }
    }

    fun setParticipantsFromJson(json: String?) {
        participants = json?.let {
            val type = object : TypeToken<Map<String, String>>() {}.type
            Gson().fromJson(it, type)
        }
    }
}