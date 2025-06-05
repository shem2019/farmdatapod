package com.example.farmdatapod.utils

import android.content.Context
import android.content.SharedPreferences

class SharedPrefs(context: Context) {
    private val PREFS_NAME = "com.example.roomdb.prefs"

    // Keys
    private companion object {
        const val TOKEN_KEY = "token"
        const val REFRESH_TOKEN_KEY = "refresh_token"
        const val USER_ID_KEY = "user_id"
        const val EMAIL_KEY = "email"
        const val PASSWORD_KEY = "password"
        const val PRODUCE_ID_COUNTER_KEY = "produce_id_counter"
        const val IS_LOGGED_IN_KEY = "is_logged_in"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Authentication related functions
    fun saveToken(token: String) {
        prefs.edit().putString(TOKEN_KEY, token).apply()
    }

    fun getToken(): String = prefs.getString(TOKEN_KEY, "") ?: ""

    fun saveRefreshToken(refreshToken: String) {
        prefs.edit().putString(REFRESH_TOKEN_KEY, refreshToken).apply()
    }

    fun getRefreshToken(): String = prefs.getString(REFRESH_TOKEN_KEY, "") ?: ""

    fun saveUserId(userId: String) {
        prefs.edit().putString(USER_ID_KEY, userId).apply()
    }

    fun getUserId(): String = prefs.getString(USER_ID_KEY, "") ?: ""

    fun saveCredentials(email: String, password: String) {
        prefs.edit().apply {
            putString(EMAIL_KEY, email)
            putString(PASSWORD_KEY, password)
            apply()
        }
    }

    fun getEmail(): String = prefs.getString(EMAIL_KEY, "") ?: ""

    fun getPassword(): String = prefs.getString(PASSWORD_KEY, "") ?: ""

    // Login state management
    fun setLoggedIn(isLoggedIn: Boolean) {
        prefs.edit().putBoolean(IS_LOGGED_IN_KEY, isLoggedIn).apply()
    }

    fun isLoggedIn(): Boolean = prefs.getBoolean(IS_LOGGED_IN_KEY, false)

    // Produce ID counter
    fun saveProduceIdCounter(counter: Int) {
        prefs.edit().putInt(PRODUCE_ID_COUNTER_KEY, counter).apply()
    }

    fun getProduceIdCounter(): Int = prefs.getInt(PRODUCE_ID_COUNTER_KEY, 6)

    // Clear authentication data
    fun clearToken() {
        prefs.edit().remove(TOKEN_KEY).apply()
    }

    fun clearRefreshToken() {
        prefs.edit().remove(REFRESH_TOKEN_KEY).apply()
    }

    fun clearUserId() {
        prefs.edit().remove(USER_ID_KEY).apply()
    }

    fun clearCredentials() {
        prefs.edit().apply {
            remove(EMAIL_KEY)
            remove(PASSWORD_KEY)
            apply()
        }
    }

    // Clear all auth related data
    fun clearAllAuthData() {
        prefs.edit().apply {
            remove(TOKEN_KEY)
            remove(REFRESH_TOKEN_KEY)
            remove(USER_ID_KEY)
            remove(EMAIL_KEY)
            remove(PASSWORD_KEY)
            remove(IS_LOGGED_IN_KEY)
            apply()
        }
    }

    // Check if we have valid auth data
    fun hasValidAuthData(): Boolean {
        return !getToken().isNullOrEmpty() &&
                !getUserId().isNullOrEmpty() &&
                isLoggedIn()
    }

    // For debugging purposes
    fun getAllAuthData(): Map<String, String?> {
        return mapOf(
            "token" to getToken(),
            "refreshToken" to getRefreshToken(),
            "userId" to getUserId(),
            "email" to getEmail(),
            "isLoggedIn" to isLoggedIn().toString()
        )
    }
}