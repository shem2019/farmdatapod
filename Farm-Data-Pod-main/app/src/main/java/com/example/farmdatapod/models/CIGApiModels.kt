package com.example.farmdatapod.models

import com.google.gson.annotations.SerializedName

/**
 * This class represents the JSON object to be SENT TO the server for CIG creation.
 * It is the single source of truth for the API request body.
 */
data class CIGCreateRequest(
    @SerializedName("cig_name") val cigName: String,
    val hub: String,
    @SerializedName("no_of_members") val numberOfMembers: Int,
    @SerializedName("date_established") val dateEstablished: String,
    val constitution: String,
    val registration: String,
    val certificate: String,
    @SerializedName("membership_register") val membershipRegister: String,
    @SerializedName("elections_held") val electionsHeld: String,
    @SerializedName("date_of_last_elections") val dateOfLastElections: String,
    @SerializedName("meeting_venue") val meetingVenue: String,
    val frequency: String,
    @SerializedName("scheduled_meeting_day") val scheduledMeetingDay: String,
    @SerializedName("scheduled_meeting_time") val scheduledMeetingTime: String,
    val members: List<MemberRequest>
)

/**
 * This represents a single member object inside the CIGCreateRequest.
 */
data class MemberRequest(
    @SerializedName("other_name") val otherName: String,
    @SerializedName("last_name") val lastName: String,
    val gender: String,
    @SerializedName("date_of_birth") val dateOfBirth: String,
    val email: String,
    @SerializedName("phone_number") val phoneNumber: Long,
    @SerializedName("id_number") val idNumber: Long,
    @SerializedName("product_involved") val productInvolved: String,
    @SerializedName("hectorage_registered_under_cig") val hectorageRegisteredUnderCig: String
)

/**
 * This class represents the successful JSON response received FROM the server.
 */
data class CIGServerResponse(
    val id: Int, // The new ID from the server
    @SerializedName("cig_name") val cigName: String
    // Add other fields if the server returns them and you need them
)