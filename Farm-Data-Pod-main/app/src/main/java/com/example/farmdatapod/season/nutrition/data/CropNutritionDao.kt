package com.example.farmdatapod.season.nutrition.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow





@Dao
interface CropNutritionDao {
    // Crop Nutrition Operations
    @Insert
    suspend fun insertCropNutrition(cropNutrition: CropNutritionEntity): Long

    @Update
    suspend fun updateCropNutrition(cropNutrition: CropNutritionEntity)

    @Delete
    suspend fun deleteCropNutrition(cropNutrition: CropNutritionEntity)

    @Query("SELECT * FROM crop_nutrition ORDER BY id DESC")
    fun getAllCropNutrition(): Flow<List<CropNutritionEntity>>

    @Query("SELECT * FROM crop_nutrition WHERE id = :id")
    suspend fun getCropNutritionById(id: Long): CropNutritionEntity?

    @Query("SELECT * FROM crop_nutrition WHERE season = :season")
    fun getCropNutritionBySeason(season: String): Flow<List<CropNutritionEntity>>

    @Query("SELECT * FROM crop_nutrition WHERE producer = :producer")
    fun getCropNutritionByProducer(producer: String): Flow<List<CropNutritionEntity>>

    @Query("SELECT * FROM crop_nutrition WHERE syncStatus = 0 OR syncStatus IS NULL")
    suspend fun getUnsyncedCropNutrition(): List<CropNutritionEntity>
    @Query("UPDATE crop_nutrition SET syncStatus = 1, serverId = :serverId WHERE id = :localId")
    suspend fun markAsSynced(localId: Long, serverId: Long)

    @Query("UPDATE crop_nutrition SET serverId = :serverId, syncStatus = 1, lastSynced = :lastSynced WHERE id = :localId")
    suspend fun markAsSynced(localId: Int, serverId: Int, lastSynced: Long = System.currentTimeMillis())

    @Query("DELETE FROM crop_nutrition")
    suspend fun deleteAllCropNutrition()

    // Applicant Operations
    @Insert
    suspend fun insertApplicant(applicant: ApplicantEntity): Long

    @Insert
    suspend fun insertApplicants(applicants: List<ApplicantEntity>)

    @Update
    suspend fun updateApplicant(applicant: ApplicantEntity)

    @Delete
    suspend fun deleteApplicant(applicant: ApplicantEntity)

    @Query("SELECT * FROM applicants WHERE cropNutritionId = :cropNutritionId")
    fun getApplicantsByCropNutritionId(cropNutritionId: Long): Flow<List<ApplicantEntity>>

    @Query("DELETE FROM applicants WHERE cropNutritionId = :cropNutritionId")
    suspend fun deleteApplicantsForCropNutrition(cropNutritionId: Long)

    // Combined Operations
    @Transaction
    @Query("SELECT * FROM crop_nutrition")
    fun getCropNutritionWithApplicants(): Flow<List<CropNutritionWithApplicants>>

    @Transaction
    suspend fun insertCropNutritionWithApplicants(
        cropNutrition: CropNutritionEntity,
        applicants: List<ApplicantEntity>
    ): Long {
        val id = insertCropNutrition(cropNutrition)
        applicants.forEach { applicant ->
            insertApplicant(applicant.copy(cropNutritionId = id))
        }
        return id
    }
}






