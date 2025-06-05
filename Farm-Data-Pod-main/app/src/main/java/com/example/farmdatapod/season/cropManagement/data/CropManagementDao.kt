package com.example.farmdatapod.season.cropManagement.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction


import androidx.room.*

@Dao
interface CropManagementDao {
    @Transaction
    @Query("SELECT * FROM crop_management")
    suspend fun getAllCropManagementWithActivities(): List<CropManagementWithActivities>

    @Transaction
    @Query("SELECT * FROM crop_management WHERE id = :id")
    suspend fun getCropManagementWithActivitiesById(id: Int): CropManagementWithActivities?

    @Transaction
    @Query("SELECT * FROM crop_management WHERE syncStatus = 0")
    suspend fun getUnsyncedCropManagement(): List<CropManagementWithActivities>

    @Query("""
        SELECT * FROM crop_management 
        WHERE producer = :producer 
        AND season = :season 
        AND field = :field 
        AND date = :date
        LIMIT 1
    """)
    suspend fun getCropManagementByFields(
        producer: String,
        season: String,
        field: String,
        date: String
    ): CropManagementEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCropManagement(cropManagement: CropManagementEntity): Long

    @Update
    suspend fun updateCropManagement(cropManagement: CropManagementEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGappingActivity(activity: GappingActivityEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeedingActivity(activity: WeedingActivityEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPruningActivity(activity: PruningActivityEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStakingActivity(activity: StakingActivityEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertThinningActivity(activity: ThinningActivityEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWateringActivity(activity: WateringActivityEntity): Long

    @Transaction
    suspend fun insertCropManagementWithActivities(
        cropManagement: CropManagementEntity,
        gappingActivities: List<GappingActivityEntity>,
        weedingActivities: List<WeedingActivityEntity>,
        pruningActivities: List<PruningActivityEntity>,
        stakingActivities: List<StakingActivityEntity>,
        thinningActivities: List<ThinningActivityEntity>,
        wateringActivities: List<WateringActivityEntity>
    ): Long {
        val cropManagementId = insertCropManagement(cropManagement)

        gappingActivities.forEach { activity ->
            insertGappingActivity(activity.copy(cropManagementId = cropManagementId))
        }
        weedingActivities.forEach { activity ->
            insertWeedingActivity(activity.copy(cropManagementId = cropManagementId))
        }
        pruningActivities.forEach { activity ->
            insertPruningActivity(activity.copy(cropManagementId = cropManagementId))
        }
        stakingActivities.forEach { activity ->
            insertStakingActivity(activity.copy(cropManagementId = cropManagementId))
        }
        thinningActivities.forEach { activity ->
            insertThinningActivity(activity.copy(cropManagementId = cropManagementId))
        }
        wateringActivities.forEach { activity ->
            insertWateringActivity(activity.copy(cropManagementId = cropManagementId))
        }

        return cropManagementId
    }

    @Query("""
        UPDATE crop_management 
        SET syncStatus = 1, 
            serverId = :serverId,
            lastSynced = :timestamp 
        WHERE id = :id
    """)
    suspend fun markAsSynced(id: Int, serverId: String?, timestamp: Long)

    @Query("DELETE FROM crop_management")
    suspend fun deleteAllCropManagement()

    @Query("""
        SELECT cm.* FROM crop_management cm
        GROUP BY cm.producer, cm.season, cm.field, cm.date
        HAVING COUNT(*) > 1
    """)
    suspend fun findDuplicateActivities(): List<CropManagementEntity>

    @Query("""
        DELETE FROM crop_management 
        WHERE id NOT IN (
            SELECT MIN(id) 
            FROM crop_management 
            GROUP BY producer, season, field, date
        )
    """)
    suspend fun deleteDuplicateActivities(): Int


    @Query("""
        UPDATE crop_management 
        SET syncStatus = 1, 
            serverId = :serverId,
            lastSynced = :timestamp,
            lastModified = :timestamp
        WHERE id = :id
    """)
    suspend fun markAsSynced(id: Long, serverId: String?, timestamp: Long)

    // Update other methods to use Long for id parameters
    @Transaction
    @Query("SELECT * FROM crop_management WHERE id = :id")
    suspend fun getCropManagementWithActivitiesById(id: Long): CropManagementWithActivities?

}