package com.example.farmdatapod.network

import com.example.farmdatapod.utils.SharedPrefs
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val sharedPrefs: SharedPrefs) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        val token = sharedPrefs.getToken() // Assuming you have a getToken method in SharedPrefs
        val newRequest = originalRequest.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()

        return chain.proceed(newRequest)
    }
}