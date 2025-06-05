package com.example.farmdatapod.hub.hubAggregation.buyingCenter.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "buying_centers")
data class BuyingCenterEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "server_id")
    val serverId: Int? = null,

    // Hub related fields
    @ColumnInfo(name = "hub")
    val hub: String? = null,

    @ColumnInfo(name = "hub_id")
    val hubId: Int,

    // Location fields
    @ColumnInfo(name = "county")
    val county: String,

    @ColumnInfo(name = "sub_county")
    val subCounty: String,

    @ColumnInfo(name = "ward")
    val ward: String,

    @ColumnInfo(name = "village")
    val village: String,

    // Buying center details
    @ColumnInfo(name = "buying_center_name")
    val buyingCenterName: String,

    @ColumnInfo(name = "buying_center_code")
    val buyingCenterCode: String,

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

    @ColumnInfo(name = "location")
    val location: String,

    // User and sync fields
    @ColumnInfo(name = "user_id")
    val userId: String?,

    @ColumnInfo(name = "sync_status")
    val syncStatus: Boolean = false,

    @ColumnInfo(name = "sync_error")
    val syncError: String? = null,

    @ColumnInfo(name = "last_sync_attempt")
    val lastSyncAttempt: Long? = null,

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

    // Timestamps
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "last_modified")
    val lastModified: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "deleted")
    val deleted: Boolean = false,

    @ColumnInfo(name = "delete_sync_status")
    val deleteSyncStatus: Boolean = false
)