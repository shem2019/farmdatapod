package com.example.farmdatapod.network

import android.util.Log
import com.example.farmdatapod.utils.TokenManager
import okhttp3.Interceptor
import okhttp3.Response

/**
 * A simple interceptor that attaches an Authorization token to a request.
 * It assumes the token is valid, as the decision to use this interceptor
 * means the call requires authentication.
 */
class AuthInterceptor(private val tokenManager: TokenManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenManager.getToken()

        // If the token is missing for a call that is supposed to be authenticated,
        // log a critical error. The request will likely fail on the server with 401/403.
        if (token.isNullOrEmpty()) {
            Log.e("AuthInterceptor", "CRITICAL: Request to ${chain.request().url} requires a token, but none was found.")
            // Proceed with the request, allowing the server to reject it.
            return chain.proceed(chain.request())
        }

        val authenticatedRequest = chain.request().newBuilder()
            .header("Authorization", "Bearer $token")
            .build()

        return chain.proceed(authenticatedRequest)
    }
}