package com.example.farmdatapod.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.farmdatapod.FarmDataPodApplication
import com.example.farmdatapod.models.LoginRequest
import com.example.farmdatapod.models.LoginResponse
import com.example.farmdatapod.models.LoginState
import com.example.farmdatapod.network.RestClient
import com.example.farmdatapod.utils.SharedPrefs // Keep for email/password if needed for offline
import com.example.farmdatapod.utils.TokenManager // Added
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class LoginViewModel(application: Application) : AndroidViewModel(application) {
    // SharedPrefs might still be used for storing username/password for offline convenience
    // but not for the session token itself.
    private val sharedPrefs = SharedPrefs(application)
    private val apiService = RestClient.getApiService(application)
    private val tokenManager = (application as FarmDataPodApplication).tokenManager // New

    private val _loginState = MutableLiveData<LoginState>()
    val loginState: LiveData<LoginState> = _loginState

    fun loginUser(email: String, password: String) {
        _loginState.value = LoginState.Loading

        val loginRequest = LoginRequest(email, password)
        val call = apiService.loginUser(loginRequest)

        call.enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if (response.isSuccessful) {
                    response.body()?.let { loginResponseBody ->
                        // Save credentials for offline convenience if desired
                        sharedPrefs.saveCredentials(email, password)
                        handleLoginSuccess(loginResponseBody)
                    } ?: handleLoginError(Exception("Empty response body"))
                } else {
                    val errorMessage = when (response.code()) {
                        401 -> "Invalid email or password"
                        403 -> "Account locked. Please contact support"
                        404 -> "Service not found"
                        500 -> "Server error. Please try again later"
                        else -> "Error code: ${response.code()}"
                    }
                    handleLoginError(Exception(errorMessage))
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                handleLoginError(t as Exception)
            }
        })
    }

    private fun handleLoginSuccess(response: LoginResponse) {
        try {
            tokenManager.saveToken(response.access_token)
            // If you want to save user_id, decide if it goes into TokenManager's SharedPreferences
            // or a separate SharedPrefs for user profile data.
            // For simplicity, if it's tied to the session, TokenManager's prefs are fine.
            // Example: sharedPrefs.saveUserId(response.user_id) // Or move to TokenManager if it makes sense
            _loginState.postValue(LoginState.Success)
        } catch (e: Exception) {
            handleLoginError(e)
        }
    }

    private fun handleLoginError(error: Exception) {
        val errorMessage = when (error) {
            is SocketTimeoutException -> "Connection timed out. Please check your internet connection"
            is UnknownHostException -> "No internet connection"
            else -> error.message ?: "An unexpected error occurred"
        }
        _loginState.postValue(LoginState.Error(errorMessage))
    }

    fun checkOfflineLogin(email: String, password: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            val storedEmail = sharedPrefs.getEmail()
            val storedPassword = sharedPrefs.getPassword()

            // For offline login, also check if there's a non-expired token.
            // This provides a layer of security: even if credentials match, if the token
            // period has lapsed, it might be better to force online login when possible.
            // However, for pure offline fallback on credentials:
            if (email == storedEmail && password == storedPassword) {
                // Optionally, you could also check tokenManager.isTokenValid() here
                // if you want to allow offline access only if a recent token was also present.
                _loginState.postValue(LoginState.Success)
            } else {
                _loginState.postValue(LoginState.Error("Invalid offline credentials"))
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            tokenManager.clearToken() // Use TokenManager
            // sharedPrefs.clearUserId() // If you manage this separately
            sharedPrefs.setLoggedIn(false) // This flag might still be useful for UI state
            // but isTokenValid should be the source of truth for sessions.
        }
    }

    fun isUserLoggedIn(): Boolean {
        // The primary source of truth for being "logged in" for session purposes
        // should now be the TokenManager's state.
        return tokenManager.isTokenValid()
    }
}