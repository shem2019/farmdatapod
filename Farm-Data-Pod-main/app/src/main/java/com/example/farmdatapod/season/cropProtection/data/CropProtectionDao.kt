package com.example.farmdatapod.season.cropProtection.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.farmdatapod.models.NameOfApplicants
import kotlinx.coroutines.flow.Flow

@Dao
interface CropProtectionDao {
    @Transaction
    @Query("SELECT * FROM crop_protection ORDER BY lastModified DESC")
    fun getAllCropProtections(): Flow<List<CropProtectionWithApplicants>>

    @Transaction
    @Query("SELECT * FROM crop_protection WHERE season_planning_id = :seasonPlanningId ORDER BY date DESC")
    fun getCropProtectionsBySeasonId(seasonPlanningId: Long): Flow<List<CropProtectionWithApplicants>>

    @Transaction
    @Query("SELECT * FROM crop_protection WHERE id = :id")
    fun getCropProtectionById(id: Long): Flow<CropProtectionWithApplicants?>

    // Added query to get unsynchronized records
    @Transaction
    @Query("SELECT * FROM crop_protection WHERE syncStatus = 0")
    fun getUnsyncedCropProtections(): Flow<List<CropProtectionWithApplicants>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCropProtection(cropProtection: CropProtectionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApplicants(applicants: List<CropProtectionApplicantEntity>)

    // Added to insert multiple crop protections at once
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCropProtections(cropProtections: List<CropProtectionEntity>)

    @Transaction
    suspend fun insertCropProtectionWithApplicants(
        cropProtection: CropProtectionEntity,
        applicants: List<NameOfApplicants>
    ): Long {
        val protectionId = insertCropProtection(cropProtection)
        val applicantEntities = applicants.map { applicant ->
            CropProtectionApplicantEntity(
                crop_protection_id = protectionId,
                name = applicant.name,
                ppes_used = applicant.ppes_used,
                equipment_used = applicant.equipment_used,
                syncStatus = false,
                lastModified = System.currentTimeMillis()
            )
        }
        insertApplicants(applicantEntities)
        return protectionId
    }

    @Delete
    suspend fun deleteCropProtection(cropProtection: CropProtectionEntity)

    @Query("DELETE FROM crop_protection WHERE id = :id")
    suspend fun deleteCropProtectionById(id: Long)

    @Query("DELETE FROM crop_protection")
    suspend fun deleteAllCropProtections()

    // Added to delete all applicants
    @Query("DELETE FROM crop_protection_applicants")
    suspend fun deleteAllApplicants()

    @Update
    suspend fun updateCropProtection(cropProtection: CropProtectionEntity)

    @Update
    suspend fun updateApplicants(applicants: List<CropProtectionApplicantEntity>)

    @Transaction
    suspend fun updateCropProtectionWithApplicants(
        cropProtection: CropProtectionEntity,
        applicants: List<NameOfApplicants>
    ) {
        updateCropProtection(cropProtection.copy(lastModified = System.currentTimeMillis(), syncStatus = false))
        val applicantEntities = applicants.map { applicant ->
            CropProtectionApplicantEntity(
                crop_protection_id = cropProtection.id,
                name = applicant.name,
                ppes_used = applicant.ppes_used,
                equipment_used = applicant.equipment_used,
                syncStatus = false,
                lastModified = System.currentTimeMillis()
            )
        }
        deleteApplicantsForProtection(cropProtection.id)
        insertApplicants(applicantEntities)
    }

    @Query("DELETE FROM crop_protection_applicants WHERE crop_protection_id = :protectionId")
    suspend fun deleteApplicantsForProtection(protectionId: Long)

    // Added for sync functionality
    @Query("UPDATE crop_protection SET syncStatus = 1, lastSynced = :timestamp WHERE id = :id")
    suspend fun markAsSynced(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE crop_protection_applicants SET syncStatus = 1, lastSynced = :timestamp WHERE crop_protection_id = :protectionId")
    suspend fun markApplicantsAsSynced(protectionId: Long, timestamp: Long = System.currentTimeMillis())

    // Added to get total number of records
    @Query("SELECT COUNT(*) FROM crop_protection")
    fun getCropProtectionCount(): Flow<Int>

    // Added to search crop protections
    @Transaction
    @Query("SELECT * FROM crop_protection WHERE product LIKE '%' || :searchQuery || '%' OR producer LIKE '%' || :searchQuery || '%'")
    fun searchCropProtections(searchQuery: String): Flow<List<CropProtectionWithApplicants>>
}