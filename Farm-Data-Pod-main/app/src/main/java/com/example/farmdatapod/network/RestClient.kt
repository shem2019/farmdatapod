package com.example.farmdatapod.network

import android.content.Context
import com.example.farmdatapod.utils.TokenManager // Import TokenManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RestClient {
    private const val BASE_URL = "https://farmdatapod.net/"
    private const val MPESA_BASE_URL = "https://sandbox.safaricom.co.ke/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY // Use Level.BODY for detailed logs, NONE for production
    }

    // Client for M-Pesa (presumably does not need auth token from TokenManager)
    private val mpesaClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    // Helper function to create a Retrofit instance
    // It will now conditionally add the AuthInterceptor
    private fun getRetrofitInstance(context: Context, tokenManager: TokenManager?): Retrofit {
        val httpClientBuilder = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)

        // If a TokenManager is provided, add the AuthInterceptor
        tokenManager?.let {
            httpClientBuilder.addInterceptor(AuthInterceptor(it, context.applicationContext))
        }

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClientBuilder.build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * Provides an ApiService instance for NON-AUTHENTICATED calls.
     */
    fun getApiService(context: Context): ApiService {
        // This version does NOT include the AuthInterceptor
        return getRetrofitInstance(context, null).create(ApiService::class.java)
    }

    /**
     * Provides an ApiService instance for AUTHENTICATED calls.
     * It requires the application-scoped TokenManager.
     */
    fun getApiService(context: Context, tokenManager: TokenManager): ApiService {
        // This version WILL include the AuthInterceptor using the provided TokenManager
        return getRetrofitInstance(context, tokenManager).create(ApiService::class.java)
    }

    // M-Pesa service - assuming it does not need our app's auth token
    fun getMpesaApiService(): MpesaApiService {
        return Retrofit.Builder()
            .baseUrl(MPESA_BASE_URL)
            .client(mpesaClient) // Uses its own client
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MpesaApiService::class.java)
    }
}