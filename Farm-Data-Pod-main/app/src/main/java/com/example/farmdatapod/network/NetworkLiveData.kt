package com.example.farmdatapod.network

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.lifecycle.LiveData
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL

class NetworkLiveData(private val application: Application) : LiveData<Boolean>() {

    private val connectivityManager = application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            Log.d("NetworkLiveData", "Network available: $network")
            postValue(true)
            checkInternetConnection()
        }

        override fun onLost(network: Network) {
            Log.d("NetworkLiveData", "Network lost: $network")
            postValue(false)
        }
    }

    override fun onActive() {
        super.onActive()
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)

        // Check the current network status and set the initial value
        checkInternetConnection()
        Log.d("NetworkLiveData", "NetworkLiveData active")
    }

    override fun onInactive() {
        super.onInactive()
        connectivityManager.unregisterNetworkCallback(networkCallback)
        Log.d("NetworkLiveData", "NetworkLiveData inactive")
    }

    private fun isNetworkConnected(): Boolean {
        val networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        return networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

    private fun checkInternetConnection() {
        if (isNetworkConnected()) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val url = URL("https://www.google.com")
                    val urlConnection = url.openConnection() as HttpURLConnection
                    urlConnection.setRequestProperty("User-Agent", "Android")
                    urlConnection.connectTimeout = 1500
                    urlConnection.readTimeout = 1500
                    urlConnection.connect()

                    if (urlConnection.responseCode == 200) {
                        postValue(true)
                    } else {
                        postValue(false)
                    }
                    urlConnection.disconnect()
                } catch (e: Exception) {
                    postValue(false)
                }
            }
        } else {
            postValue(false)
        }
    }
}