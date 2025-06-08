package com.example.farmdatapod.sync

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.farmdatapod.FarmDataPodApplication
import com.example.farmdatapod.auth.LoginActivity
import com.example.farmdatapod.utils.TokenManager

class AutoSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    private val TAG = "AutoSyncWorker"
    private val tokenManager = (appContext.applicationContext as FarmDataPodApplication).tokenManager
    private val syncManager = (appContext.applicationContext as FarmDataPodApplication).syncManager


    override suspend fun doWork(): Result {
        try {
            if (!tokenManager.isTokenValid()) {
                Log.d(TAG, "Token expired, logging out")
                logoutUser()
                return Result.success()
            }

            Log.d(TAG, "Starting periodic auto sync")
            val syncResult = syncManager.performFullSync()

            // Log detailed results for each entity
            syncResult.results.forEach { entityResult ->
                val status = when {
                    entityResult.error != null -> "Failed with error: ${entityResult.error}"
                    entityResult.successful -> "Success"
                    else -> "Failed"
                }

                Log.d(TAG, """
                    Sync results for ${entityResult.entityName}:
                    Status: $status
                    Uploaded: ${entityResult.uploadedCount}
                    Downloaded: ${entityResult.downloadedCount}
                    Failures: ${entityResult.failures}
                """.trimIndent())
            }

            // If there was an overall error, log it
            syncResult.error?.let { error ->
                Log.e(TAG, "Overall sync error: $error")
            }

            // Determine if we should retry based on results
            return when {
                syncResult.error != null -> {
                    Log.e(TAG, "Sync failed with error: ${syncResult.error}")
                    Result.retry()
                }
                !syncResult.isSuccessful() -> {
                    Log.e(TAG, "Sync completed with some failures")
                    Result.retry()
                }
                else -> {
                    Log.d(TAG, "Auto sync completed successfully")
                    Result.success()
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Auto sync failed with exception", e)
            return Result.retry()
        }
    }

    private fun logoutUser() {
        try {
            // Clear token
            tokenManager.clearTokenAndExpiry()

            // Start login activity
            val intent = Intent(applicationContext, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            applicationContext.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error during logout", e)
        }
    }
}

// Data classes for tracking sync results
