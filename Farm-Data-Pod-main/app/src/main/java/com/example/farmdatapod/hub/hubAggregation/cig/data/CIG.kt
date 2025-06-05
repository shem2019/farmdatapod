package com.example.farmdatapod.hub.hubAggregation.cig.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cigs")
data class CIG(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "server_id")
    val serverId: Int? = null,

    @ColumnInfo(name = "cig_name")
    val cigName: String,

    @ColumnInfo(name = "hub")
    val hub: String,

    @ColumnInfo(name = "no_of_members")
    val numberOfMembers: Int,

    @ColumnInfo(name = "date_established")
    val dateEstablished: String,

    @ColumnInfo(name = "constitution")
    val constitution: String?,

    @ColumnInfo(name = "registration")
    val registration: String?,

    @ColumnInfo(name = "elections_held")
    val electionsHeld: String?,

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
    val syncStatus: Boolean = false,

    // Member fields
    @ColumnInfo(name = "member_other_name")
    val memberOtherName: String,

    @ColumnInfo(name = "member_last_name")
    val memberLastName: String,

    @ColumnInfo(name = "member_gender")
    val memberGender: String,

    @ColumnInfo(name = "member_date_of_birth")
    val memberDateOfBirth: String,

    @ColumnInfo(name = "member_email")
    val memberEmail: String,

    @ColumnInfo(name = "member_phone_number")
    val memberPhoneNumber: Long,

    @ColumnInfo(name = "member_id_number")
    val memberIdNumber: Int,

    @ColumnInfo(name = "product_involved")
    val productInvolved: String,

    @ColumnInfo(name = "hectorage_registered_under_cig")
    val hectorageRegisteredUnderCig: String,

    @ColumnInfo(name = "cig_id")
    val cigId: Int? = null
)

