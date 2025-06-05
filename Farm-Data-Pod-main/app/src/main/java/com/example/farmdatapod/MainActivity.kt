package com.example.farmdatapod

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.lifecycle.lifecycleScope
import com.example.farmdatapod.auth.LoginActivity
import com.example.farmdatapod.sync.SyncManager
import com.example.farmdatapod.utils.TokenManager
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var syncManager: SyncManager
    private lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize managers
        syncManager = SyncManager(this)
        tokenManager = TokenManager(this)

        // Setup periodic sync if user is logged in
        if (tokenManager.isTokenValid()) {
            setupSync()
        }

        // UI Setup
        setupUI()

        // Navigation Setup
        handleNavigation()
    }

    private fun setupSync() {
        try {
            // Setup periodic background sync
            syncManager.setupPeriodicSync()

            // Perform immediate sync
            lifecycleScope.launch {
                try {
                    val result = syncManager.performFullSync()  // Direct call instead of syncNow
                    Log.d(TAG, "Initial sync result: $result")
                } catch (e: Exception) {
                    Log.e(TAG, "Initial sync failed", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up sync", e)
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
                syncManager.cancelAllSync()

                // Clear authentication token
                tokenManager.clearToken()

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
        if (tokenManager.isTokenValid()) {
            lifecycleScope.launch {
                try {
                    syncManager.syncNow()
                } catch (e: Exception) {
                    Log.e(TAG, "Sync on resume failed", e)
                }
            }
        }
    }
}