package com.example.farmdatapod.network


import com.example.farmdatapod.models.AccessTokenResponse
import com.example.farmdatapod.models.B2CPaymentRequest
import com.example.farmdatapod.models.B2CPaymentResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface MpesaApiService {
    @POST("mpesa/b2c/v1/paymentrequest")
    suspend fun makeB2CPayment(
        @Header("Authorization") authHeader: String,
        @Body request: B2CPaymentRequest
    ): B2CPaymentResponse

    @GET("oauth/v1/generate?grant_type=client_credentials")
    suspend fun getAccessToken(
        @Header("Authorization") authHeader: String
    ): AccessTokenResponse
}
