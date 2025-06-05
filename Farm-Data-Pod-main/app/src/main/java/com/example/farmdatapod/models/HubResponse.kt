package com.example.farmdatapod.models

import com.google.gson.annotations.SerializedName

data class HubResponse(
    @SerializedName("forms") val forms: List<Hub>
)

data class Hub(
    @SerializedName("id") val id: Int,
    @SerializedName("region") val region: String,
    @SerializedName("hub_name") val hubName: String,
    @SerializedName("hub_code") val hubCode: String,
    @SerializedName("address") val address: String,
    @SerializedName("year_established") val yearEstablished: String,
    @SerializedName("ownership") val ownership: String,
    @SerializedName("floor_size") val floorSize: String,
    @SerializedName("facilities") val facilities: String,
    @SerializedName("input_center") val inputCenter: String,
    @SerializedName("type_of_building") val typeOfBuilding: String,
    @SerializedName("longitude") val longitude: String,
    @SerializedName("latitude") val latitude: String,
    @SerializedName("key_contacts") val keyContacts: List<HubFormKeyContact>,
    @SerializedName("user_id") val userId: String
)

data class HubFormKeyContact(
    @SerializedName("other_name") val otherName: String,
    @SerializedName("last_name") val lastName: String,
    @SerializedName("gender") val gender: String,
    @SerializedName("role") val role: String,
    @SerializedName("date_of_birth") val dateOfBirth: String,
    @SerializedName("email") val email: String,
    @SerializedName("phone_number") val phoneNumber: String,
    @SerializedName("id_number") val idNumber: Int,
    @SerializedName("hub_id") val hubId: Int,
    @SerializedName("buying_center_id") val buyingCenterId: Int
)