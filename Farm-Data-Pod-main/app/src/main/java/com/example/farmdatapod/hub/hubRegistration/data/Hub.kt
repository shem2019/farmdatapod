package com.example.farmdatapod.hub.hubRegistration.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "hubs",
    indices = [Index(
        value = ["hub_name", "hub_code"],
        unique = true  // This ensures no duplicates with same name and code
    )]
)
data class Hub(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "server_id")
    val serverId: Int? = null,

    @ColumnInfo(name = "region")
    val region: String,

    @ColumnInfo(name = "hub_name")
    val hubName: String,

    @ColumnInfo(name = "hub_code")
    val hubCode: String,

    @ColumnInfo(name = "address")
    val address: String,

    @ColumnInfo(name = "year_established")
    val yearEstablished: String,

    @ColumnInfo(name = "ownership")
    val ownership: String,

    @ColumnInfo(name = "floor_size")
    val floorSize: String,

    @ColumnInfo(name = "facilities")
    val facilities: String,

    @ColumnInfo(name = "input_center")
    val inputCenter: String,

    @ColumnInfo(name = "type_of_building")
    val typeOfBuilding: String,

    @ColumnInfo(name = "longitude")
    val longitude: String,

    @ColumnInfo(name = "latitude")
    val latitude: String,

    @ColumnInfo(name = "user_id")
    val userId: String,

    @ColumnInfo(name = "sync_status")
    val syncStatus: Boolean = false,

    // Key Contact fields
    @ColumnInfo(name = "contact_other_name")
    val contactOtherName: String,

    @ColumnInfo(name = "contact_last_name")
    val contactLastName: String,

    @ColumnInfo(name = "contact_gender")
    val contactGender: String,

    @ColumnInfo(name = "contact_role")
    val contactRole: String,

    @ColumnInfo(name = "contact_date_of_birth")
    val contactDateOfBirth: String,

    @ColumnInfo(name = "contact_email")
    val contactEmail: String,

    @ColumnInfo(name = "contact_phone_number")
    val contactPhoneNumber: String,

    @ColumnInfo(name = "contact_id_number")
    val contactIdNumber: Int,

    @ColumnInfo(name = "hub_id")
    val hubId: Int? = null,

    @ColumnInfo(name = "buying_center_id")
    val buyingCenterId: Int? = null,

    // New fields for sync tracking
    @ColumnInfo(name = "last_modified")
    val lastModified: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "last_synced")
    val lastSynced: Long = 0
)