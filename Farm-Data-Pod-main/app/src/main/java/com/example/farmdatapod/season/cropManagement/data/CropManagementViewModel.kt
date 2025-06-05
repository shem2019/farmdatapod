package com.example.farmdatapod.season.cropManagement.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.farmdatapod.models.CropManagementModel
import com.example.farmdatapod.sync.SyncStats
import com.example.farmdatapod.utils.NetworkUtils
import kotlinx.coroutines.launch

class CropManagementViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: CropManagementRepository = CropManagementRepository(application)

    private val _savingStatus = MutableLiveData<Result<CropManagementModel>>()
    val savingStatus: LiveData<Result<CropManagementModel>> = _savingStatus

    private val _syncStatus = MutableLiveData<Result<SyncStats>>()
    val syncStatus: LiveData<Result<SyncStats>> = _syncStatus

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun saveCropManagement(cropManagement: CropManagementModel) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val isOnline = NetworkUtils.isNetworkAvailable(getApplication())
                val result = repository.saveCropManagement(cropManagement, isOnline)
                _savingStatus.value = result

                // If save was successful and we're offline, schedule a sync
                if (result.isSuccess && !isOnline) {
                    _syncStatus.value = Result.success(SyncStats(0, 0, 0, false))
                }
            } catch (e: Exception) {
                _savingStatus.value = Result.failure(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun syncData() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                if (!NetworkUtils.isNetworkAvailable(getApplication())) {
                    _syncStatus.value = Result.failure(Exception("No network connection available"))
                    return@launch
                }

                val result = repository.performFullSync()
                _syncStatus.value = result
            } catch (e: Exception) {
                _syncStatus.value = Result.failure(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun syncUnsynced() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                if (!NetworkUtils.isNetworkAvailable(getApplication())) {
                    _syncStatus.value = Result.failure(Exception("No network connection available"))
                    return@launch
                }

                val result = repository.syncUnsynced()
                _syncStatus.value = result
            } catch (e: Exception) {
                _syncStatus.value = Result.failure(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun syncFromServer() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                if (!NetworkUtils.isNetworkAvailable(getApplication())) {
                    _syncStatus.value = Result.failure(Exception("No network connection available"))
                    return@launch
                }

                val result = repository.syncFromServer()
                _syncStatus.value = Result.success(SyncStats(0, 0, result.getOrDefault(0), true))
            } catch (e: Exception) {
                _syncStatus.value = Result.failure(e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}