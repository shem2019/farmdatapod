package com.example.farmdatapod.season.landPreparation.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LandPreparationDao {
    @Transaction
    @Query("SELECT * FROM land_preparation")
    fun getAllLandPreparations(): Flow<List<LandPreparationWithDetails>>

    @Transaction
    @Query("SELECT * FROM land_preparation WHERE id = :id")
    suspend fun getLandPreparationById(id: Long): LandPreparationWithDetails?

    @Transaction
    @Query("SELECT * FROM land_preparation WHERE syncStatus = 0")
    fun getUnsyncedLandPreparations(): Flow<List<LandPreparationWithDetails>>

    @Query("SELECT * FROM land_preparation WHERE producerId = :producerId")
    fun getLandPreparationsByProducer(producerId: Int): Flow<List<LandPreparationWithDetails>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLandPreparation(landPrep: LandPreparationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCoverCrop(coverCrop: CoverCropEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMulching(mulching: MulchingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSoilAnalysis(soilAnalysis: SoilAnalysisEntity)

    @Transaction
    suspend fun insertLandPreparationWithDetails(
        landPrep: LandPreparationEntity,
        coverCrop: CoverCropEntity?,
        mulching: MulchingEntity?,
        soilAnalysis: SoilAnalysisEntity?
    ): Long {
        val landPrepId = insertLandPreparation(landPrep)

        coverCrop?.let {
            insertCoverCrop(it.copy(landPrepId = landPrepId))
        }

        mulching?.let {
            insertMulching(it.copy(landPrepId = landPrepId))
        }

        soilAnalysis?.let {
            insertSoilAnalysis(it.copy(landPrepId = landPrepId))
        }

        return landPrepId
    }

    @Update
    suspend fun updateLandPreparation(landPrep: LandPreparationEntity)

    @Update
    suspend fun updateCoverCrop(coverCrop: CoverCropEntity)

    @Update
    suspend fun updateMulching(mulching: MulchingEntity)

    @Update
    suspend fun updateSoilAnalysis(soilAnalysis: SoilAnalysisEntity)

    @Query("UPDATE land_preparation SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, status: Boolean)

    @Delete
    suspend fun deleteLandPreparation(landPrep: LandPreparationEntity)

    @Query("DELETE FROM land_preparation")
    suspend fun deleteAllLandPreparations()
}