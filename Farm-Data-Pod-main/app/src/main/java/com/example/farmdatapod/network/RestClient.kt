package com.example.farmdatapod.network

import android.content.Context
import com.example.farmdatapod.utils.SharedPrefs
import com.example.farmdatapod.utils.TokenManager // Import TokenManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RestClient {
    private const val BASE_URL = "https://farmdatapod.net/" //
    private const val MPESA_BASE_URL = "https://sandbox.safaricom.co.ke/" //

    private val loggingInterceptor = HttpLoggingInterceptor().apply { //
        level = HttpLoggingInterceptor.Level.BODY //
    }

    @Volatile private var apiService: ApiService? = null
    @Volatile private var mpesaApiService: MpesaApiService? = null

    /**
     * Creates and returns an OkHttpClient instance with the AuthInterceptor.
     * The AuthInterceptor is initialized with TokenManager to retrieve the token.
     * @param context The application context.
     * @return An OkHttpClient configured with logging and authentication.
     */
    private fun createOkHttpClient(context: Context): OkHttpClient {
        // Step 1: Initialize SharedPrefs (the primary storage layer)
        val sharedPrefs = SharedPrefs(context)
        // Step 2: Initialize TokenManager (the token logic layer) using SharedPrefs for storage
        val tokenManager = TokenManager(sharedPrefs)
        // Step 3: Initialize AuthInterceptor (the network interception layer) using TokenManager for token access
        val authInterceptor = AuthInterceptor(tokenManager)

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor) //
            .addInterceptor(authInterceptor) // Add the correctly initialized AuthInterceptor
            .connectTimeout(60, TimeUnit.SECONDS) //
            .readTimeout(60, TimeUnit.SECONDS) //
            .writeTimeout(60, TimeUnit.SECONDS) //
            .build() //
    }

    /**
     * Creates and returns an OkHttpClient instance for Mpesa API calls.
     * This client does not include the AuthInterceptor as Mpesa API typically uses different authentication.
     * @return An OkHttpClient configured with logging for Mpesa API.
     */
    private fun createMpesaOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor) //
            .connectTimeout(60, TimeUnit.SECONDS) //
            .readTimeout(60, TimeUnit.SECONDS) //
            .writeTimeout(60, TimeUnit.SECONDS) //
            .build() //
    }

    /**
     * Provides the singleton instance of ApiService.
     * The service is lazily initialized the first time it's requested.
     * @param context The application context.
     * @return The ApiService instance.
     */
    fun getApiService(context: Context): ApiService { //
        return apiService ?: synchronized(this) {
            apiService ?: buildApiService(context).also { apiService = it }
        }
    }

    /**
     * Builds the ApiService instance.
     * @param context The application context.
     * @return The newly built ApiService instance.
     */
    private fun buildApiService(context: Context): ApiService {
        val client = createOkHttpClient(context) // Client is built using the new createOkHttpClient
        return Retrofit.Builder()
            .baseUrl(BASE_URL) //
            .client(client) //
            .addConverterFactory(GsonConverterFactory.create()) //
            .build() //
            .create(ApiService::class.java) //
    }

    /**
     * Provides the singleton instance of MpesaApiService.
     * The service is lazily initialized the first time it's requested.
     * @return The MpesaApiService instance.
     */
    fun getMpesaApiService(): MpesaApiService { //
        return mpesaApiService ?: synchronized(this) {
            mpesaApiService ?: buildMpesaApiService().also { mpesaApiService = it }
        }
    }

    /**
     * Builds the MpesaApiService instance.
     * @return The newly built MpesaApiService instance.
     */
    private fun buildMpesaApiService(): MpesaApiService {
        val client = createMpesaOkHttpClient() //
        return Retrofit.Builder()
            .baseUrl(MPESA_BASE_URL) //
            .client(client) //
            .addConverterFactory(GsonConverterFactory.create()) //
            .build() //
            .create(MpesaApiService::class.java) //
    }
}