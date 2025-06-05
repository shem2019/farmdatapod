package com.example.farmdatapod.season.forecastYields.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface YieldForecastDao {
    @Query("SELECT * FROM yield_forecasts ORDER BY createdAt DESC")
    fun getAllYieldForecasts(): Flow<List<YieldForecast>>

    @Query("SELECT * FROM yield_forecasts WHERE syncStatus = 0")
    suspend fun getUnsyncedYieldForecasts(): List<YieldForecast>

    @Query("SELECT * FROM yield_forecasts WHERE id = :id")
    suspend fun getYieldForecastById(id: Int): YieldForecast?

    @Query("SELECT * FROM yield_forecasts WHERE seasonPlanningId = :seasonId")
    fun getYieldForecastsBySeasonId(seasonId: Int): Flow<List<YieldForecast>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertYieldForecast(yieldForecast: YieldForecast): Long

    @Query("UPDATE yield_forecasts SET syncStatus = 1, serverId = :serverId WHERE id = :localId")
    suspend fun markAsSynced(localId: Int, serverId: Int)

    @Query("DELETE FROM yield_forecasts")
    suspend fun deleteAll()

    @Query("DELETE FROM yield_forecasts WHERE syncStatus = 0")
    suspend fun deleteUnsyncedForecasts()
}