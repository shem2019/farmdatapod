package com.example.farmdatapod.hub.hubAggregation.cig.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cigs")
data class CIG(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "server_id")
    var serverId: Int? = null,

    @ColumnInfo(name = "cig_name")
    val cigName: String,

    val hub: String,

    @ColumnInfo(name = "no_of_members")
    val numberOfMembers: Int,

    @ColumnInfo(name = "date_established")
    val dateEstablished: String,

    val constitution: String?,
    val registration: String?,
    val certificate: String?,
    @ColumnInfo(name = "membership_register")
    val membershipRegister: String,
    @ColumnInfo(name = "elections_held")
    val electionsHeld: String?,
    @ColumnInfo(name = "date_of_last_elections")
    val dateOfLastElections: String,
    @ColumnInfo(name = "meeting_venue")
    val meetingVenue: String,

    // Renamed to match API for consistency
    @ColumnInfo(name = "meeting_frequency")
    val meetingFrequency: String,

    @ColumnInfo(name = "scheduled_meeting_day")
    val scheduledMeetingDay: String,

    @ColumnInfo(name = "scheduled_meeting_time")
    val scheduledMeetingTime: String,

    // Added new fields required by the API
    @ColumnInfo(name = "membership_contribution_amount")
    val membershipContributionAmount: String,

    @ColumnInfo(name = "membership_contribution_frequency")
    val membershipContributionFrequency: String,

    @ColumnInfo(name = "user_id")
    val userId: String?,

    @ColumnInfo(name = "sync_status")
    var syncStatus: Boolean = false,

    @ColumnInfo(name = "members_json")
    val membersJson: String?
)