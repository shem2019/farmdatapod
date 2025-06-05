package com.example.farmdatapod.cropmanagement.forecastYield.data


import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.farmdatapod.season.forecastYields.data.YieldForecast
import kotlinx.coroutines.launch

class YieldForecastManagementViewModel(private val repository: YieldForecastManagementRepository) : ViewModel() {
    private val _submitStatus = MutableLiveData<SubmitResult>()
    val submitStatus: LiveData<SubmitResult> = _submitStatus

    private val _yieldForecasts = MutableLiveData<List<YieldForecast>>()
    val yieldForecasts: LiveData<List<YieldForecast>> = _yieldForecasts

    private val _syncStatus = MutableLiveData<SyncResult>()
    val syncStatus: LiveData<SyncResult> = _syncStatus

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun submitYieldForecast(forecast: YieldForecast, isOnline: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = repository.saveYieldForecast(forecast, isOnline)
                result.fold(
                    onSuccess = {
                        _submitStatus.value = SubmitResult.Success
                        if (isOnline) {
                            syncWithServer()
                        }
                    },
                    onFailure = { _submitStatus.value = SubmitResult.Error(it.message ?: "Unknown error") }
                )
            } catch (e: Exception) {
                _submitStatus.value = SubmitResult.Error(e.message ?: "Unknown error")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun syncWithServer() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // First sync unsynced records
                repository.syncUnsynced()
                // Then get updates from server
                val result = repository.syncFromServer()
                result.fold(
                    onSuccess = { _syncStatus.value = SyncResult.Success(it) },
                    onFailure = { _syncStatus.value = SyncResult.Error(it.message ?: "Unknown error") }
                )
            } catch (e: Exception) {
                _syncStatus.value = SyncResult.Error(e.message ?: "Sync failed")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearUnsyncedData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.clearUnsyncedForecasts()
                _syncStatus.value = SyncResult.Success(0)
            } catch (e: Exception) {
                _syncStatus.value = SyncResult.Error("Failed to clear unsynced data")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearOldData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.clearUnsyncedForecasts()
                _syncStatus.value = SyncResult.Success(0)
                syncWithServer() // Refresh after clearing
            } catch (e: Exception) {
                _syncStatus.value = SyncResult.Error("Failed to clear old data: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    sealed class SubmitResult {
        object Success : SubmitResult()
        data class Error(val message: String) : SubmitResult()
    }

    sealed class SyncResult {
        data class Success(val count: Int) : SyncResult()
        data class Error(val message: String) : SyncResult()
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(YieldForecastManagementViewModel::class.java)) {
                return YieldForecastManagementViewModel(YieldForecastManagementRepository(context)) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}