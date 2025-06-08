package com.example.farmdatapod.hub.hubAggregation.cig.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cigs")
data class CIG(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "server_id")
    var serverId: Int? = null, // Made 'var' to be updatable after sync

    @ColumnInfo(name = "cig_name")
    val cigName: String,

    @ColumnInfo(name = "hub")
    val hub: String,

    @ColumnInfo(name = "no_of_members")
    val numberOfMembers: Int,

    @ColumnInfo(name = "date_established")
    val dateEstablished: String,

    @ColumnInfo(name = "constitution")
    val constitution: String?, // This will store the URL or "No"

    @ColumnInfo(name = "registration")
    val registration: String?, // This will store the URL or "No"

    @ColumnInfo(name = "certificate")
    val certificate: String?, // This will store the URL or "No"

    @ColumnInfo(name = "membership_register") // This matches the API field name
    val membershipRegister: String,

    @ColumnInfo(name = "elections_held")
    val electionsHeld: String?, // This will store the URL or "No"

    @ColumnInfo(name = "date_of_last_elections")
    val dateOfLastElections: String,

    @ColumnInfo(name = "meeting_venue")
    val meetingVenue: String,

    @ColumnInfo(name = "frequency")
    val frequency: String,

    @ColumnInfo(name = "scheduled_meeting_day")
    val scheduledMeetingDay: String,

    @ColumnInfo(name = "scheduled_meeting_time")
    val scheduledMeetingTime: String,

    @ColumnInfo(name = "user_id")
    val userId: String?,

    @ColumnInfo(name = "sync_status")
    var syncStatus: Boolean = false, // Made 'var' to be updatable

    // THIS IS THE KEY CHANGE:
    // We will store the entire list of members as a single JSON string.
    @ColumnInfo(name = "members_json")
    val membersJson: String? = null
)