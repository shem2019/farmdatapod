package com.example.farmdatapod.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.farmdatapod.models.LoginRequest
import com.example.farmdatapod.models.LoginResponse
import com.example.farmdatapod.models.LoginState
import com.example.farmdatapod.network.RestClient
import com.example.farmdatapod.utils.SharedPrefs
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
    private val sharedPrefs = SharedPrefs(application)
    private val apiService = RestClient.getApiService(application)

    private val _loginState = MutableLiveData<LoginState>()
    val loginState: LiveData<LoginState> = _loginState

    fun loginUser(email: String, password: String) {
        _loginState.value = LoginState.Loading

        val loginRequest = LoginRequest(email, password)
        val call = apiService.loginUser(loginRequest)

        call.enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if (response.isSuccessful) {
                    response.body()?.let { handleLoginSuccess(it) }
                        ?: handleLoginError(Exception("Empty response body"))
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
            with(sharedPrefs) {
                saveToken(response.access_token)
                saveRefreshToken(response.refresh_token)
                saveUserId(response.user_id)
                setLoggedIn(true)
            }
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
            try {
                _loginState.value = LoginState.Loading

                val storedEmail = sharedPrefs.getEmail()
                val storedPassword = sharedPrefs.getPassword()

                if (email == storedEmail && password == storedPassword) {
                    _loginState.postValue(LoginState.Success)
                } else {
                    _loginState.postValue(LoginState.Error("Invalid offline credentials"))
                }
            } catch (e: Exception) {
                _loginState.postValue(LoginState.Error("Failed to verify offline credentials"))
            }
        }
    }

  /* fun clearLoginState() {
        _loginState.value = null
    }*/

    fun logout() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                with(sharedPrefs) {
                    clearToken()
                    clearRefreshToken()
                    clearUserId()
                    setLoggedIn(false)
                }
            }
        }
    }

    fun isUserLoggedIn(): Boolean {
        return sharedPrefs.isLoggedIn() && !sharedPrefs.getToken().isNullOrEmpty()
    }
}