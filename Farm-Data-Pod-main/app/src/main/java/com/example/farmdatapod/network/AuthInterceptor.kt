package com.example.farmdatapod.network

import com.example.farmdatapod.utils.TokenManager // Changed import to TokenManager
import okhttp3.Interceptor
import okhttp3.Response

// AuthInterceptor now depends on TokenManager for token access and validation
class AuthInterceptor(private val tokenManager: TokenManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response { //
        val originalRequest = chain.request() //

        // Get token from the TokenManager, which handles validity checks
        val token = tokenManager.getToken() // Now correctly using TokenManager's getToken()
        val newRequest = originalRequest.newBuilder()
            .header("Authorization", "Bearer $token") //
            .build() //

        return chain.proceed(newRequest) //
    }
}