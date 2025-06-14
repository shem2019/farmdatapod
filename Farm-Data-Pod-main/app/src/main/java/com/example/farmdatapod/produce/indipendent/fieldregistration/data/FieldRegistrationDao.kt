package com.example.farmdatapod.produce.indipendent.fieldregistration.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface FieldRegistrationDao {
    @Transaction
    @Query("SELECT * FROM field_registrations ORDER BY created_at DESC")
    fun getAllFieldRegistrations(): Flow<List<FieldRegistrationWithCrops>>

    @Transaction
    @Query("SELECT * FROM field_registrations WHERE id = :id")
    suspend fun getFieldRegistrationById(id: Int): FieldRegistrationWithCrops?

    @Transaction // <-- Add this
    @Query("SELECT * FROM field_registrations WHERE server_id = :serverId")
    suspend fun getFieldRegistrationByServerId(serverId: Int): FieldRegistrationWithCrops?

    @Query("SELECT * FROM field_registrations WHERE producer_id = :producerId")
    fun getFieldRegistrationsByProducer(producerId: String): Flow<List<FieldRegistrationWithCrops>>

    @Transaction // <-- Add this
    @Query("SELECT * FROM field_registrations WHERE sync_status = 0")
    suspend fun getUnsyncedFieldRegistrations(): List<FieldRegistrationWithCrops>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFieldRegistration(fieldRegistration: FieldRegistrationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrop(crop: CropEntity): Long

    @Transaction
    suspend fun insertFieldRegistrationWithCrops(
        fieldRegistration: FieldRegistrationEntity,
        crops: List<CropEntity>
    ) {
        // Check for existing server_id to prevent duplicates from sync
        if (fieldRegistration.serverId != null) {
            val existing = getFieldRegistrationByServerId(fieldRegistration.serverId)
            if (existing != null) {
                // Optionally update existing record or just return
                return
            }
        }
        val fieldRegistrationId = insertFieldRegistration(fieldRegistration)
        crops.forEach { crop ->
            insertCrop(crop.copy(fieldRegistrationId = fieldRegistrationId.toInt()))
        }
    }

    @Query("UPDATE field_registrations SET sync_status = :status WHERE id = :id")
    suspend fun updateFieldRegistrationSyncStatus(id: Int, status: Boolean)

    @Query("UPDATE field_registrations SET server_id = :serverId WHERE id = :localId")
    suspend fun updateFieldRegistrationServerId(localId: Int, serverId: Int)

    @Query("UPDATE crops SET sync_status = :status WHERE field_registration_id = :fieldRegistrationId")
    suspend fun updateCropsSyncStatus(fieldRegistrationId: Int, status: Boolean)

    @Delete
    suspend fun deleteFieldRegistration(fieldRegistration: FieldRegistrationEntity)

    @Query("DELETE FROM field_registrations WHERE id = :id")
    suspend fun deleteFieldRegistrationById(id: Int)
}