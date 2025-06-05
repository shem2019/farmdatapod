package com.example.farmdatapod.network

import android.content.Context
import com.example.farmdatapod.utils.SharedPrefs
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RestClient {
//    private const val BASE_URL = "https://deploy-run.onrender.com"
    private const val BASE_URL = "https://farmdatapod.net/"

    private const val MPESA_BASE_URL = "https://sandbox.safaricom.co.ke/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private lateinit var sharedPrefs: SharedPrefs
    private lateinit var authInterceptor: AuthInterceptor

    private val client: OkHttpClient
        get() = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(authInterceptor)
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

    private val mpesaClient: OkHttpClient
        get() = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

    fun getApiService(context: Context): ApiService {
        sharedPrefs = SharedPrefs(context)
        authInterceptor = AuthInterceptor(sharedPrefs)
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    fun getMpesaApiService(): MpesaApiService {
        return Retrofit.Builder()
            .baseUrl(MPESA_BASE_URL)
            .client(mpesaClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MpesaApiService::class.java)
    }
}