package com.example.farmdatapod.logistics.createRoute.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RouteDao {
    // Insert a new route
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoute(route: RouteEntity): Long

    // Update existing route
    @Update
    suspend fun updateRoute(route: RouteEntity)

    // Delete route
    @Delete
    suspend fun deleteRoute(route: RouteEntity)

    // Get single route by ID
    @Query("SELECT * FROM routes WHERE id = :routeId AND isDeleted = 0")
    suspend fun getRouteById(routeId: Long): RouteEntity?

    @Query("SELECT * FROM routes WHERE routeNumber = :number AND isDeleted = 0")
    suspend fun getRouteByNumber(number: String): RouteEntity?

    // Get all routes (not deleted) - using Flow for reactive updates
    @Query("SELECT * FROM routes WHERE isDeleted = 0 ORDER BY createdAt DESC")
    fun getAllRoutes(): Flow<List<RouteEntity>>

    // Get routes by sync status
    @Query("SELECT * FROM routes WHERE syncStatus = :status AND isDeleted = 0")
    suspend fun getRoutesBySyncStatus(status: String): List<RouteEntity>

    // Update sync status
    @Query("UPDATE routes SET syncStatus = :status, syncMessage = :message, syncedAt = :syncTime WHERE id = :routeId")
    suspend fun updateSyncStatus(routeId: Long, status: String, message: String?, syncTime: Long?)

    // Soft delete route
    @Query("UPDATE routes SET isDeleted = 1, updatedAt = :timestamp WHERE id = :routeId")
    suspend fun softDeleteRoute(routeId: Long, timestamp: Long = System.currentTimeMillis())

    // Get routes that need syncing (either new or updated)
    @Query("SELECT * FROM routes WHERE syncStatus = 'PENDING' AND isDeleted = 0")
    suspend fun getPendingRoutes(): List<RouteEntity>

    // Update route number
    @Query("UPDATE routes SET routeNumber = :newRouteNumber, updatedAt = :timestamp WHERE id = :routeId")
    suspend fun updateRouteNumber(routeId: Long, newRouteNumber: String, timestamp: Long = System.currentTimeMillis())

    // Count routes with specific sync status
    @Query("SELECT COUNT(*) FROM routes WHERE syncStatus = :status AND isDeleted = 0")
    suspend fun getCountByStatus(status: String): Int
}