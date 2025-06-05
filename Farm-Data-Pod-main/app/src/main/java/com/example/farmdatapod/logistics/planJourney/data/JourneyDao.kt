package com.example.farmdatapod.logistics.planJourney.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface JourneyDao {

    @Query("""
        SELECT 
            j.id as journey_id,
            j.driver || ' - ' || j.date_and_time as journey_name
        FROM journeys j
        ORDER BY j.date_and_time DESC
    """)
    suspend fun getJourneyNamesAndIds(): List<JourneyBasicInfo>

    // Journey with its stop points information
    @Query("""
        SELECT 
            j.id as journey_id,
            j.driver || ' - ' || j.date_and_time as journey_name,
            sp.id as stop_point_id,
            sp.stop_point as stop_point_name
        FROM journeys j
        LEFT JOIN stop_points sp ON j.id = sp.journey_id
        ORDER BY j.date_and_time DESC, sp.time ASC
    """)
    suspend fun getJourneysWithStopPointInfo(): List<JourneyStopPointInfo>

    // Duplicate Handling Operations
    @Query("""
        SELECT j1.*
        FROM journeys j1
        INNER JOIN (
            SELECT date_and_time, driver, truck, route_id, COUNT(*)
            FROM journeys
            GROUP BY date_and_time, driver, truck, route_id
            HAVING COUNT(*) > 1
        ) j2
        ON j1.date_and_time = j2.date_and_time
        AND j1.driver = j2.driver
        AND j1.truck = j2.truck
        AND j1.route_id = j2.route_id
    """)
    suspend fun findDuplicateJourneys(): List<JourneyEntity>

    @Query("""
        DELETE FROM journeys
        WHERE id IN (
            SELECT j1.id
            FROM journeys j1
            INNER JOIN (
                SELECT date_and_time, driver, truck, route_id, MIN(id) as min_id
                FROM journeys
                GROUP BY date_and_time, driver, truck, route_id
                HAVING COUNT(*) > 1
            ) j2
            ON j1.date_and_time = j2.date_and_time
            AND j1.driver = j2.driver
            AND j1.truck = j2.truck
            AND j1.route_id = j2.route_id
            AND j1.id != j2.min_id
        )
    """)
    suspend fun deleteDuplicateJourneys(): Int

    // Journey Operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJourney(journey: JourneyEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJourneys(journeys: List<JourneyEntity>)

    @Update
    suspend fun updateJourney(journey: JourneyEntity)

    @Delete
    suspend fun deleteJourney(journey: JourneyEntity)

    @Query("DELETE FROM journeys WHERE id = :journeyId")
    suspend fun deleteJourneyById(journeyId: Long)

    @Query("DELETE FROM journeys")
    suspend fun deleteAllJourneys()

    @Query("SELECT * FROM journeys WHERE id = :journeyId")
    suspend fun getJourneyById(journeyId: Long): JourneyEntity?

    @Query("SELECT * FROM journeys ORDER BY date_and_time DESC")
    fun getAllJourneysFlow(): Flow<List<JourneyEntity>>

    @Query("SELECT * FROM journeys ORDER BY date_and_time DESC")
    suspend fun getAllJourneys(): List<JourneyEntity>

    @Query("SELECT * FROM journeys WHERE syncStatus = 0 ORDER BY lastModified DESC")
    suspend fun getUnsyncedJourneys(): List<JourneyEntity>

    // StopPoint Operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStopPoint(stopPoint: StopPointEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStopPoints(stopPoints: List<StopPointEntity>)



    @Update
    suspend fun updateStopPoint(stopPoint: StopPointEntity)

    @Delete
    suspend fun deleteStopPoint(stopPoint: StopPointEntity)

    @Query("DELETE FROM stop_points WHERE journey_id = :journeyId")
    suspend fun deleteStopPointsByJourneyId(journeyId: Long)

    @Query("SELECT * FROM stop_points WHERE journey_id = :journeyId ORDER BY time ASC")
    suspend fun getStopPointsByJourneyId(journeyId: Long): List<StopPointEntity>

    @Query("SELECT * FROM stop_points WHERE syncStatus = 0 ORDER BY lastModified DESC")
    suspend fun getUnsyncedStopPoints(): List<StopPointEntity>

    // Relationship Queries
    @Transaction
    @Query("SELECT * FROM journeys WHERE id = :journeyId")
    suspend fun getJourneyWithStopPoints(journeyId: Long): JourneyWithStopPoints?

    @Transaction
    @Query("SELECT * FROM journeys ORDER BY date_and_time DESC")
    fun getAllJourneysWithStopPointsFlow(): Flow<List<JourneyWithStopPoints>>

    @Transaction
    @Query("SELECT * FROM journeys ORDER BY date_and_time DESC")
    suspend fun getAllJourneysWithStopPoints(): List<JourneyWithStopPoints>

    // Sync Related Operations
    @Query("SELECT j.*, sp.stop_point as stop_name, sp.id as stop_id FROM journeys j " +
            "LEFT JOIN stop_points sp ON j.id = sp.journey_id " +
            "WHERE j.id = :journeyId")
    suspend fun getJourneyWithStopPointDetails(journeyId: String): JourneyWithStopPoints?

    @Query("UPDATE stop_points SET syncStatus = :syncStatus, lastSynced = :lastSynced, serverId = :serverId WHERE id = :id")
    suspend fun updateStopPointSyncStatus(id: Long, syncStatus: Boolean, lastSynced: Long, serverId: Long)

    @Query("SELECT driver, COUNT(*) as count FROM journeys GROUP BY driver ORDER BY count DESC")
    suspend fun getDriverJourneyCounts(): List<DriverCount>

    @Query("SELECT * FROM journeys WHERE route_id = :routeId")
    suspend fun getJourneyByRouteId(routeId: Long): JourneyWithStopPoints?

    @Query("SELECT * FROM journeys WHERE server_id = :serverId")
    suspend fun getJourneyByServerId(serverId: Long): JourneyWithStopPoints?
    @Query("UPDATE journeys SET syncStatus = :syncStatus, lastSynced = :lastSynced, server_id = :serverId WHERE id = :id")
    suspend fun updateJourneySyncStatus(id: Long, syncStatus: Boolean, lastSynced: Long, serverId: Long)

    // Transaction Methods
    @Transaction
    suspend fun insertJourneyWithStopPoints(journey: JourneyEntity, stopPoints: List<StopPointEntity>) {
        val journeyId = insertJourney(journey)
        stopPoints.forEach { stopPoint ->
            insertStopPoint(stopPoint.copy(journey_id = journeyId))
        }
    }

    @Transaction
    suspend fun updateJourneyWithStopPoints(journeyWithStopPoints: JourneyWithStopPoints) {
        updateJourney(journeyWithStopPoints.journey)
        journeyWithStopPoints.stopPoints.forEach { stopPoint ->
            updateStopPoint(stopPoint)
        }
    }

    // Search and Filter Methods
    @Query("SELECT * FROM journeys WHERE driver LIKE '%' || :searchQuery || '%' OR truck LIKE '%' || :searchQuery || '%' ORDER BY date_and_time DESC")
    fun searchJourneys(searchQuery: String): Flow<List<JourneyEntity>>

    @Query("SELECT * FROM journeys WHERE date_and_time BETWEEN :startDate AND :endDate ORDER BY date_and_time DESC")
    suspend fun getJourneysByDateRange(startDate: String, endDate: String): List<JourneyEntity>

    @Query("SELECT * FROM journeys WHERE logistician_status = :status ORDER BY date_and_time DESC")
    fun getJourneysByStatus(status: String): Flow<List<JourneyEntity>>

    // Statistics and Analytics
    @Query("SELECT COUNT(*) FROM journeys")
    suspend fun getJourneyCount(): Int

    @Query("SELECT COUNT(*) FROM stop_points WHERE journey_id = :journeyId")
    suspend fun getStopPointsCountForJourney(journeyId: Long): Int
}

data class DriverCount(
    val driver: String,
    val count: Int
)