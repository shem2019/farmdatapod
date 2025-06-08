package com.example.farmdatapod.utils

import android.content.Context
import android.content.SharedPreferences

class SharedPrefs(context: Context) {
    private val PREFS_NAME = "com.example.roomdb.prefs" //

    // Keys already present in your SharedPrefs.kt
    private companion object {
        const val TOKEN_KEY = "token" //
        const val REFRESH_TOKEN_KEY = "refresh_token" //
        const val USER_ID_KEY = "user_id" //
        const val EMAIL_KEY = "email" //
        const val PASSWORD_KEY = "password" //
        const val PRODUCE_ID_COUNTER_KEY = "produce_id_counter" //
        const val IS_LOGGED_IN_KEY = "is_logged_in" //
        // NEW: Key for token expiry, to be managed by TokenManager
        const val TOKEN_EXPIRY_KEY = "token_expiry"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) //

    // Authentication related functions (existing)
    fun saveToken(token: String) { //
        prefs.edit().putString(TOKEN_KEY, token).apply() //
    }

    fun getToken(): String = prefs.getString(TOKEN_KEY, "") ?: "" //

    fun saveRefreshToken(refreshToken: String) { //
        prefs.edit().putString(REFRESH_TOKEN_KEY, refreshToken).apply() //
    }

    fun getRefreshToken(): String = prefs.getString(REFRESH_TOKEN_KEY, "") ?: "" //

    fun saveUserId(userId: String) { //
        prefs.edit().putString(USER_ID_KEY, userId).apply() //
    }

    fun getUserId(): String = prefs.getString(USER_ID_KEY, "") ?: "" //

    fun saveCredentials(email: String, password: String) { //
        prefs.edit().apply {
            putString(EMAIL_KEY, email) //
            putString(PASSWORD_KEY, password) //
            apply() //
        }
    }

    fun getEmail(): String = prefs.getString(EMAIL_KEY, "") ?: "" //

    fun getPassword(): String = prefs.getString(PASSWORD_KEY, "") ?: "" //

    // Login state management (existing)
    fun setLoggedIn(isLoggedIn: Boolean) { //
        prefs.edit().putBoolean(IS_LOGGED_IN_KEY, isLoggedIn).apply() //
    }

    fun isLoggedIn(): Boolean = prefs.getBoolean(IS_LOGGED_IN_KEY, false) //

    // Produce ID counter (existing)
    fun saveProduceIdCounter(counter: Int) { //
        prefs.edit().putInt(PRODUCE_ID_COUNTER_KEY, counter).apply() //
    }

    fun getProduceIdCounter(): Int = prefs.getInt(PRODUCE_ID_COUNTER_KEY, 6) //

    // Clear authentication data (existing)
    fun clearToken() { //
        prefs.edit().remove(TOKEN_KEY).apply() //
    }

    fun clearRefreshToken() { //
        prefs.edit().remove(REFRESH_TOKEN_KEY).apply() //
    }

    fun clearUserId() { //
        prefs.edit().remove(USER_ID_KEY).apply() //
    }

    fun clearCredentials() { //
        prefs.edit().apply {
            remove(EMAIL_KEY) //
            remove(PASSWORD_KEY) //
            apply() //
        }
    }

    // Clear all auth related data (existing)
    fun clearAllAuthData() { //
        prefs.edit().apply {
            remove(TOKEN_KEY) //
            remove(REFRESH_TOKEN_KEY) //
            remove(USER_ID_KEY) //
            remove(EMAIL_KEY) //
            remove(PASSWORD_KEY) //
            remove(IS_LOGGED_IN_KEY) //
            // NEW: Ensure expiry key is also cleared when clearing all auth data
            remove(TOKEN_EXPIRY_KEY)
            apply() //
        }
    }

    // Check if we have valid auth data (existing)
    fun hasValidAuthData(): Boolean { //
        return !getToken().isNullOrEmpty() && //
                !getUserId().isNullOrEmpty() && //
                isLoggedIn() //
    }

    // For debugging purposes (existing)
    fun getAllAuthData(): Map<String, String?> { //
        return mapOf(
            "token" to getToken(), //
            "refreshToken" to getRefreshToken(), //
            "userId" to getUserId(), //
            "email" to getEmail(), //
            "isLoggedIn" to isLoggedIn().toString() //
        )
    }

    // Generic methods for other types (existing)
    fun saveString(key: String, value: String) { //
        prefs.edit().putString(key, value).apply() //
    }

    fun getString(key: String, defaultValue: String? = null): String? { //
        return prefs.getString(key, defaultValue) //
    }

    fun saveBoolean(key: String, value: Boolean) { //
        prefs.edit().putBoolean(key, value).apply() //
    }

    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean { //
        return prefs.getBoolean(key, defaultValue) //
    }

    fun saveInt(key: String, value: Int) { //
        prefs.edit().putInt(key, value).apply() //
    }

    fun getInt(key: String, defaultValue: Int = 0): Int { //
        return prefs.getInt(key, defaultValue) //
    }

    // NEW: Methods for Long values, essential for TokenManager's expiry timestamps
    fun saveLong(key: String, value: Long) {
        prefs.edit().putLong(key, value).apply()
    }

    fun getLong(key: String, defaultValue: Long = 0L): Long {
        return prefs.getLong(key, defaultValue)
    }

    // NEW: Generic remove method to support TokenManager in clearing specific keys
    fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }
}