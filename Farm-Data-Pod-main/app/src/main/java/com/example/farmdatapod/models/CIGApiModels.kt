package com.example.farmdatapod.models

import com.google.gson.annotations.SerializedName

// NEW version of MemberRequest that matches the documentation exactly
data class MemberRequest(
    @SerializedName("other_name") val otherName: String,
    @SerializedName("last_name") val lastName: String,
    val gender: String,
    @SerializedName("date_of_birth") val dateOfBirth: String, // e.g., "1990-01-01T00:00:00"
    @SerializedName("membership_number") val membershipNumber: String,
    @SerializedName("registration_date") val registrationDate: String, // e.g., "2025-03-10T00:00:00"
    @SerializedName("share_capital") val shareCapital: Double,
    val contribution: Double,
    @SerializedName("membership_fee") val membershipFee: Double,
    @SerializedName("phone_number") val phoneNumber: String, // Sending as String is safer
    val email: String,
    @SerializedName("voting_rights") val votingRights: Boolean,
    @SerializedName("position_held") val positionHeld: String,
    @SerializedName("membership_category") val membershipCategory: String,
    @SerializedName("product_or_service") val productOrService: String
)

// NEW version of CIGCreateRequest that matches the documentation exactly
data class CIGCreateRequest(
    @SerializedName("cig_name") val cigName: String,
    val hub: String,
    @SerializedName("no_of_members") val numberOfMembers: Int,
    @SerializedName("date_established") val dateEstablished: String, // e.g., "1990-01-01T00:00:00"
    val constitution: String,
    val registration: String,
    val certificate: String,
    @SerializedName("membership_register") val membershipRegister: String,
    @SerializedName("elections_held") val electionsHeld: String,
    @SerializedName("date_of_last_elections") val dateOfLastElections: String, // e.g., "1990-01-01T00:00:00"
    @SerializedName("meeting_venue") val meetingVenue: String,
    @SerializedName("meeting_frequency") val meetingFrequency: String, // Renamed from 'frequency'
    @SerializedName("scheduled_meeting_day") val scheduledMeetingDay: String,
    @SerializedName("scheduled_meeting_time") val scheduledMeetingTime: String,
    @SerializedName("membership_contribution_amount") val membershipContributionAmount: String, // Added field
    @SerializedName("membership_contribution_frequency") val membershipContributionFrequency: String, // Added field
    val members: List<MemberRequest>
)

// The server response can likely stay the same
data class CIGServerResponse(
    val id: Int,
    @SerializedName("cig_name") val cigName: String
)