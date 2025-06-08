package com.example.farmdatapod.utils

// TokenManager.kt
// This class now orchestrates token management (expiry, validation) using SharedPrefs for storage.
class TokenManager(private val sharedPrefs: SharedPrefs) { // Accepts SharedPrefs instead of Context
    // Use the same token key as defined in SharedPrefs for consistency, and manage expiry key.
    private companion object {
        const val TOKEN_EXPIRY_KEY = "token_expiry" // This key will be managed by TokenManager
        const val TOKEN_VALIDITY_DURATION = 24 * 60 * 60 * 1000L // 24 hours in milliseconds
    }

    // No longer creating its own SharedPreferences instance:
    // private val sharedPrefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    fun saveToken(token: String) { //
        val expiryTime = System.currentTimeMillis() + TOKEN_VALIDITY_DURATION
        sharedPrefs.saveToken(token) // Use SharedPrefs's existing saveToken method
        sharedPrefs.saveLong(TOKEN_EXPIRY_KEY, expiryTime) // Use SharedPrefs for saving expiry
    }

    // Add a getToken method to TokenManager for consistent access
    fun getToken(): String? {
        return sharedPrefs.getToken() // Get token from SharedPrefs
    }

    fun isTokenValid(): Boolean { //
        val expiryTime = sharedPrefs.getLong(TOKEN_EXPIRY_KEY, 0L) // Get expiry from SharedPrefs
        // Check if token exists in SharedPrefs AND is not expired
        return !sharedPrefs.getToken().isNullOrEmpty() && System.currentTimeMillis() < expiryTime
    }

    // Renamed for clarity, as it clears both token and its expiry data
    fun clearTokenAndExpiry() {
        sharedPrefs.clearToken() // Use SharedPrefs's existing clearToken method
        sharedPrefs.remove(TOKEN_EXPIRY_KEY) // Use SharedPrefs's new remove method for expiry
    }
}