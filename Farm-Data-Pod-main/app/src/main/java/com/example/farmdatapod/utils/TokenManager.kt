package com.example.farmdatapod.utils

import android.content.Context
import android.util.Log

class TokenManager(private val context: Context) {
    private val sharedPrefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    private val TOKEN_KEY = "auth_token"
    private val TOKEN_EXPIRY_KEY = "token_expiry"
    // Consider making this configurable or longer for production
    private val TOKEN_VALIDITY_DURATION = 24 * 60 * 60 * 1000 // 24 hours

    fun saveToken(token: String) {
        val expiryTime = System.currentTimeMillis() + TOKEN_VALIDITY_DURATION
        sharedPrefs.edit()
            .putString(TOKEN_KEY, token)
            .putLong(TOKEN_EXPIRY_KEY, expiryTime)
            .apply()
        Log.d("TokenManager", "Token saved. Expires at: $expiryTime")
    }

    // Optional: If you also store refresh token with TokenManager
    // fun saveRefreshToken(refreshToken: String) { ... }

    fun getToken(): String? {
        return sharedPrefs.getString(TOKEN_KEY, null)
    }

    fun isTokenValid(): Boolean {
        val token = getToken()
        if (token.isNullOrEmpty()) {
            Log.d("TokenManager", "Token is null or empty.")
            return false
        }
        val expiryTime = sharedPrefs.getLong(TOKEN_EXPIRY_KEY, 0)
        if (expiryTime == 0L) {
            Log.d("TokenManager", "Token expiry time not found.")
            return false // No expiry time saved, consider it invalid
        }
        val isValid = System.currentTimeMillis() < expiryTime
        Log.d("TokenManager", "Token validity check: $isValid. Expires at: $expiryTime, Current time: ${System.currentTimeMillis()}")
        return isValid
    }

    fun clearToken() {
        sharedPrefs.edit()
            .remove(TOKEN_KEY)
            .remove(TOKEN_EXPIRY_KEY)
            // .remove(REFRESH_TOKEN_KEY) // If you add refresh token
            .apply()
        Log.d("TokenManager", "Token cleared.")
    }
}