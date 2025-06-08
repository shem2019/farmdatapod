package com.example.farmdatapod

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider // Added
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import com.example.farmdatapod.auth.LoginActivity
import com.example.farmdatapod.network.NetworkViewModel // Added
import com.example.farmdatapod.sync.SyncManager
import com.example.farmdatapod.utils.TokenManager
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var syncManager: SyncManager
    private lateinit var tokenManager: TokenManager
    private lateinit var networkViewModel: NetworkViewModel // Added

    // To prevent multiple syncs if network state changes rapidly
    private var isSyncing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize managers
        syncManager = (application as FarmDataPodApplication).syncManager
        tokenManager = (application as FarmDataPodApplication).tokenManager

        // Initialize NetworkViewModel using the application context from FarmDataPodApplication
        networkViewModel = ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(application)).get(NetworkViewModel::class.java) // Modified initialization


        // Setup periodic sync if user is logged in
        if (tokenManager.isTokenValid()) {
            setupSync()
        }

        // UI Setup
        setupUI()

        // Navigation Setup
        handleNavigation()

        // Observe Network Changes
        observeNetworkChanges() // Added
    }

    private fun setupSync() {
        try {
            // Setup periodic background sync
            syncManager.setupPeriodicSync()

            // Perform initial immediate sync only if network is available
            if (networkViewModel.networkLiveData.value == true && !isSyncing) {
                isSyncing = true
                lifecycleScope.launch {
                    try {
                        Log.d(TAG, "Initial sync triggered by setupSync")
                        val result = syncManager.performFullSync()
                        Log.d(TAG, "Initial sync result: $result")
                    } catch (e: Exception) {
                        Log.e(TAG, "Initial sync failed", e)
                    } finally {
                        isSyncing = false
                    }
                }
            } else {
                Log.d(TAG, "Initial sync skipped: Network not available or sync already in progress.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up sync", e)
        }
    }

    // Added method to observe network changes
    private fun observeNetworkChanges() {
        networkViewModel.networkLiveData.observe(this) { isAvailable ->
            Log.d(TAG, "Network status changed: ${if (isAvailable) "Available" else "Unavailable"}")
            if (isAvailable && tokenManager.isTokenValid() && !isSyncing) {
                isSyncing = true
                Log.i(TAG, "Network available, attempting to sync data.")
                lifecycleScope.launch {
                    try {
                        Log.d(TAG, "Sync triggered by network change")
                        val result = syncManager.performFullSync()
                        Log.d(TAG, "Sync result from network change: $result")
                        // Optionally, provide user feedback based on syncResult
                    } catch (e: Exception) {
                        Log.e(TAG, "Sync failed after network change", e)
                    } finally {
                        isSyncing = false
                    }
                }
            } else if (!isAvailable) {
                Log.i(TAG, "Network unavailable, sync will be attempted when network is back.")
            } else if (isSyncing) {
                Log.d(TAG, "Sync skipped: Another sync operation is already in progress.")
            } else if (!tokenManager.isTokenValid()) {
                Log.d(TAG, "Sync skipped: User token is not valid.")
            }
        }
    }

    private fun setupUI() {
        // Disable edge-to-edge to show the status bar
        WindowCompat.setDecorFitsSystemWindows(window, true)
        setContentView(R.layout.activity_main)

        // Set the status bar color programmatically
        window.statusBarColor = getColor(R.color.green)

        // Make the status bar icons dark (for light status bar background)
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun handleNavigation() {
        // Get a reference to the NavController from the NavHostFragment
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Handle navigation based on intent extras
        intent.getStringExtra("destination")?.let { destination ->
            if (destination == "HomePageFragment") {
                navController.navigate(R.id.homePageFragment)
            }
        }
    }

    fun logout() {
        lifecycleScope.launch {
            try {
                // Cancel all sync operations before logout
                syncManager.cancelAllSync() //

                // Clear authentication token
                tokenManager.clearToken() //

                // Navigate to login screen
                startActivity(Intent(this@MainActivity, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                finish()
            } catch (e: Exception) {
                Log.e(TAG, "Logout failed", e)
                // You might want to show an error message to the user here
            }
        }
    }

    // Optional: Handle sync scheduling when app comes back to foreground
    override fun onResume() {
        super.onResume()
        // Consider if this is still needed or if network observer is sufficient.
        // If kept, ensure it also checks the isSyncing flag.
        if (tokenManager.isTokenValid() && networkViewModel.networkLiveData.value == true && !isSyncing) { //
            isSyncing = true
            lifecycleScope.launch {
                try {
                    Log.d(TAG, "Sync triggered by onResume")
                    syncManager.syncNow() //
                } catch (e: Exception) {
                    Log.e(TAG, "Sync on resume failed", e)
                } finally {
                    isSyncing = false
                }
            }
        } else {
            Log.d(TAG, "Sync on resume skipped: Token invalid, network unavailable, or sync in progress.")
        }
    }
}