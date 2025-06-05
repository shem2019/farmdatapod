package com.example.farmdatapod.utils

import android.content.Context

// TokenManager.kt
class TokenManager(private val context: Context) {
    private val sharedPrefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    private val TOKEN_KEY = "auth_token"
    private val TOKEN_EXPIRY_KEY = "token_expiry"
    private val TOKEN_VALIDITY_DURATION = 24 * 60 * 60 * 1000 // 24 hours in milliseconds

    fun saveToken(token: String) {
        val expiryTime = System.currentTimeMillis() + TOKEN_VALIDITY_DURATION
        sharedPrefs.edit()
            .putString(TOKEN_KEY, token)
            .putLong(TOKEN_EXPIRY_KEY, expiryTime)
            .apply()
    }

    fun isTokenValid(): Boolean {
        val expiryTime = sharedPrefs.getLong(TOKEN_EXPIRY_KEY, 0)
        return System.currentTimeMillis() < expiryTime
    }

    fun clearToken() {
        sharedPrefs.edit()
            .remove(TOKEN_KEY)
            .remove(TOKEN_EXPIRY_KEY)
            .apply()
    }
}