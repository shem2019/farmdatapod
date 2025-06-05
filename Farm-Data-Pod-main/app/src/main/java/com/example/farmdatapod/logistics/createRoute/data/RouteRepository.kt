package com.example.farmdatapod.logistics.createRoute.data


import android.content.Context
import android.util.Log
import com.example.farmdatapod.models.RouteModel
import com.example.farmdatapod.network.RestClient
import com.example.farmdatapod.sync.SyncStats
import com.example.farmdatapod.sync.SyncableRepository
import com.example.farmdatapod.utils.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.coroutines.resume



import com.example.farmdatapod.models.RouteStopPoint


class RouteRepository(private val context: Context) : SyncableRepository {
    private val TAG = "RouteRepository"
    private val routeDao = AppDatabase.getInstance(context).routeDao()

    suspend fun saveRoute(route: RouteEntity, isOnline: Boolean): Result<RouteEntity> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Attempting to save route: ${route.routeNumber}, Online mode: $isOnline")

            val localId = routeDao.insertRoute(route)
            Log.d(TAG, "Route saved locally with ID: $localId")

            if (isOnline) {
                try {
                    Log.d(TAG, "Attempting to sync with server...")
                    val routeModel = createRouteModel(route)

                    context?.let { ctx ->
                        RestClient.getApiService(ctx).createRoute(routeModel)
                            .enqueue(object : Callback<RouteModel> {
                                override fun onResponse(
                                    call: Call<RouteModel>,
                                    response: Response<RouteModel>
                                ) {
                                    if (response.isSuccessful) {
                                        response.body()?.let { serverRoute ->
                                            CoroutineScope(Dispatchers.IO).launch {
                                                routeDao.updateSyncStatus(
                                                    routeId = localId,
                                                    status = "SYNCED",
                                                    message = null,
                                                    syncTime = System.currentTimeMillis()
                                                )
                                                Log.d(TAG, "Route synced with server")
                                            }
                                        }
                                    } else {
                                        Log.e(TAG, "Sync failed: ${response.errorBody()?.string()}")
                                    }
                                }

                                override fun onFailure(call: Call<RouteModel>, t: Throwable) {
                                    Log.e(TAG, "Network error during sync", t)
                                }
                            })
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error during server sync", e)
                }
            } else {
                Log.d(TAG, "Offline mode - Route will sync later")
            }
            Result.success(route)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving route", e)
            Result.failure(e)
        }
    }

    override suspend fun syncUnsynced(): Result<SyncStats> = withContext(Dispatchers.IO) {
        try {
            val unsyncedRoutes = routeDao.getRoutesBySyncStatus("PENDING")
            Log.d(TAG, "Found ${unsyncedRoutes.size} unsynced routes")

            var successCount = 0
            var failureCount = 0

            unsyncedRoutes.forEach { route ->
                try {
                    val routeModel = createRouteModel(route)
                    context?.let { ctx ->
                        RestClient.getApiService(ctx).createRoute(routeModel)
                            .enqueue(object : Callback<RouteModel> {
                                override fun onResponse(
                                    call: Call<RouteModel>,
                                    response: Response<RouteModel>
                                ) {
                                    if (response.isSuccessful) {
                                        CoroutineScope(Dispatchers.IO).launch {
                                            routeDao.updateSyncStatus(
                                                route.id,
                                                "SYNCED",
                                                null,
                                                System.currentTimeMillis()
                                            )
                                            successCount++
                                            Log.d(TAG, "Successfully synced ${route.routeNumber}")
                                        }
                                    } else {
                                        failureCount++
                                        Log.e(TAG, "Failed to sync ${route.routeNumber}")
                                    }
                                }

                                override fun onFailure(call: Call<RouteModel>, t: Throwable) {
                                    failureCount++
                                    Log.e(TAG, "Error syncing ${route.routeNumber}", t)
                                }
                            })
                    }
                } catch (e: Exception) {
                    failureCount++
                    Log.e(TAG, "Error syncing ${route.routeNumber}", e)
                }
            }

            Result.success(SyncStats(successCount, failureCount, 0, successCount > 0))
        } catch (e: Exception) {
            Log.e(TAG, "Error during sync", e)
            Result.failure(e)
        }
    }

    override suspend fun syncFromServer(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            var savedCount = 0

            return@withContext suspendCancellableCoroutine { continuation ->
                context?.let { ctx ->
                    RestClient.getApiService(ctx).getRoutes().enqueue(object : Callback<List<RouteModel>> {
                        override fun onResponse(
                            call: Call<List<RouteModel>>,
                            response: Response<List<RouteModel>>
                        ) {
                            if (response.isSuccessful) {
                                try {
                                    val routes = response.body()

                                    CoroutineScope(Dispatchers.IO).launch {
                                        try {
                                            AppDatabase.getInstance(context).runInTransaction {
                                                routes?.distinctBy { it.route_number }?.forEach { serverRoute ->
                                                    try {
                                                        launch {
                                                            processServerRoute(serverRoute)?.let { savedCount++ }
                                                        }
                                                    } catch (e: Exception) {
                                                        Log.e(TAG, "Error processing route ${serverRoute.route_number}", e)
                                                    }
                                                }
                                            }
                                            continuation.resume(Result.success(savedCount))
                                        } catch (e: Exception) {
                                            continuation.resume(Result.failure(e))
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error processing response", e)
                                    continuation.resume(Result.failure(e))
                                }
                            } else {
                                continuation.resume(Result.failure(RuntimeException("Error: ${response.code()}")))
                            }
                        }

                        override fun onFailure(call: Call<List<RouteModel>>, t: Throwable) {
                            Log.e(TAG, "Network error during sync", t)
                            continuation.resume(Result.failure(t))
                        }
                    })
                } ?: continuation.resume(Result.failure(RuntimeException("Context is null")))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing from server", e)
            Result.failure(e)
        }
    }

    private suspend fun processServerRoute(serverRoute: RouteModel): RouteEntity? {
        return try {
            val localRoute = createLocalRouteFromServer(serverRoute)
            routeDao.insertRoute(localRoute)
            localRoute
        } catch (e: Exception) {
            Log.e(TAG, "Error processing server route", e)
            null
        }
    }

    private fun createLocalRouteFromServer(serverRoute: RouteModel): RouteEntity {
        return RouteEntity(
            routeNumber = serverRoute.route_number,
            startingPoint = serverRoute.starting_point,
            finalDestination = serverRoute.final_destination,
            stopPoints = serverRoute.route_stop_points.map { RouteStopPoint(it.stop) },
            syncStatus = "SYNCED",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            syncedAt = System.currentTimeMillis()
        )
    }

    private fun createRouteModel(route: RouteEntity) = RouteModel(
        route_number = route.routeNumber,
        starting_point = route.startingPoint,
        final_destination = route.finalDestination,
        route_stop_points = route.stopPoints.map { RouteStopPoint(it.stop) }
    )

    override suspend fun performFullSync(): Result<SyncStats> = withContext(Dispatchers.IO) {
        try {
            val uploadResult = syncUnsynced()
            val downloadResult = syncFromServer()

            Result.success(
                SyncStats(
                    uploadedCount = uploadResult.getOrNull()?.uploadedCount ?: 0,
                    uploadFailures = uploadResult.getOrNull()?.uploadFailures ?: 0,
                    downloadedCount = downloadResult.getOrNull() ?: 0,
                    successful = uploadResult.isSuccess && downloadResult.isSuccess
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getAllRoutes(): Flow<List<RouteEntity>> = routeDao.getAllRoutes()

    suspend fun getRouteById(id: Long): RouteEntity? = withContext(Dispatchers.IO) {
        routeDao.getRouteById(id)
    }

    suspend fun deleteRoute(route: RouteEntity) = withContext(Dispatchers.IO) {
        routeDao.deleteRoute(route)
    }

    suspend fun updateRoute(route: RouteEntity) = withContext(Dispatchers.IO) {
        routeDao.updateRoute(route)
    }
}