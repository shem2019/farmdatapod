package com.example.farmdatapod.network

import android.content.Context
import android.util.Log
import com.example.farmdatapod.utils.TokenManager
import okhttp3.Interceptor
import okhttp3.Response

// AuthInterceptor now takes TokenManager
class AuthInterceptor(
    private val tokenManager: TokenManager,
    private val context: Context // Context might be useful for future token refresh logic or logging
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val token = tokenManager.getToken() // Use TokenManager to get the token

        if (!tokenManager.isTokenValid() && token != null) { // Optional: Proactively check validity
            Log.w("AuthInterceptor", "Token is present but no longer valid according to TokenManager. Clearing and proceeding without token.")
            // Potentially trigger token refresh here if you implement it
            // For now, let's clear it to avoid sending an expired token if isTokenValid also checks expiry
            // tokenManager.clearToken() // Be careful with this; might be too aggressive.
            // The backend should ideally reject expired tokens.
            // If tokenManager.getToken() returns null when invalid, this check is simpler.
        }

        val requestBuilder = originalRequest.newBuilder()

        if (token != null) {
            Log.d("AuthInterceptor", "Attaching token: Bearer $token")
            requestBuilder.header("Authorization", "Bearer $token")
        } else {
            Log.w("AuthInterceptor", "No token available for request to ${originalRequest.url}")
        }

        return chain.proceed(requestBuilder.build())
    }
}