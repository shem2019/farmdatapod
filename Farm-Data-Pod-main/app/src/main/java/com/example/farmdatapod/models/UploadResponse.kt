package com.example.farmdatapod.models

import com.google.gson.annotations.SerializedName

data class UploadResponse(
    @SerializedName("secure_url") val secureUrl: String
)

