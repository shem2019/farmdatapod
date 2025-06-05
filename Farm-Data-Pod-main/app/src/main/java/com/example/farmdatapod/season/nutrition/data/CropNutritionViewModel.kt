package com.example.farmdatapod.season.nutrition.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.farmdatapod.utils.NetworkUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class CropNutritionViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "CropNutritionViewModel"
    private val repository = CropNutritionRepository(application)

    private val _uiState = MutableStateFlow<UiState>(UiState.Initial)
    val uiState: StateFlow<UiState> = _uiState

    private val _cropNutritionList = MutableStateFlow<List<CropNutritionWithApplicants>>(emptyList())
    val cropNutritionList: StateFlow<List<CropNutritionWithApplicants>> = _cropNutritionList

    init {
        loadCropNutritionList()
    }

    fun saveCropNutrition(cropNutrition: CropNutritionEntity, applicants: List<ApplicantEntity>) {
        viewModelScope.launch {
            try {
                _uiState.value = UiState.Loading

                val isOnline = NetworkUtils.isNetworkAvailable(getApplication())
                repository.saveCropNutrition(cropNutrition, applicants, isOnline)
                    .onSuccess {
                        _uiState.value = UiState.Success("Crop nutrition saved successfully")
                        loadCropNutritionList()
                    }
                    .onFailure { error ->
                        _uiState.value = UiState.Error(error.message ?: "Failed to save crop nutrition")
                    }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "An unexpected error occurred")
            }
        }
    }

    fun updateCropNutrition(cropNutrition: CropNutritionEntity, applicants: List<ApplicantEntity>) {
        viewModelScope.launch {
            try {
                _uiState.value = UiState.Loading

                val isOnline = NetworkUtils.isNetworkAvailable(getApplication())
                repository.saveCropNutrition(cropNutrition, applicants, isOnline)
                    .onSuccess {
                        _uiState.value = UiState.Success("Crop nutrition updated successfully")
                        loadCropNutritionList()
                    }
                    .onFailure { error ->
                        _uiState.value = UiState.Error(error.message ?: "Failed to update crop nutrition")
                    }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "An unexpected error occurred")
            }
        }
    }

    fun syncData() {
        viewModelScope.launch {
            try {
                _uiState.value = UiState.Loading

                if (!NetworkUtils.isNetworkAvailable(getApplication())) {
                    _uiState.value = UiState.Error("No network connection available")
                    return@launch
                }

                repository.performFullSync()
                    .onSuccess { stats ->
                        val message = buildString {
                            append("Sync completed. ")
                            append("Uploaded: ${stats.uploadedCount}, ")
                            append("Failed: ${stats.uploadFailures}, ")
                            append("Downloaded: ${stats.downloadedCount}")
                        }
                        _uiState.value = UiState.Success(message)
                        loadCropNutritionList()
                    }
                    .onFailure { error ->
                        _uiState.value = UiState.Error(error.message ?: "Sync failed")
                    }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Sync failed")
            }
        }
    }

    private fun loadCropNutritionList() {
        viewModelScope.launch {
            repository.getAllCropNutrition()
                .catch { error ->
                    _uiState.value = UiState.Error(error.message ?: "Failed to load crop nutrition list")
                }
                .collect { list ->
                    _cropNutritionList.value = list
                }
        }
    }

    fun getCropNutritionById(id: Long) {
        viewModelScope.launch {
            try {
                _uiState.value = UiState.Loading
                val nutrition = repository.getCropNutritionById(id)
                if (nutrition != null) {
                    _uiState.value = UiState.Success("Crop nutrition retrieved successfully")
                } else {
                    _uiState.value = UiState.Error("Crop nutrition not found")
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to get crop nutrition")
            }
        }
    }

    fun deleteCropNutrition(cropNutrition: CropNutritionEntity) {
        viewModelScope.launch {
            try {
                _uiState.value = UiState.Loading
                repository.deleteCropNutrition(cropNutrition)
                _uiState.value = UiState.Success("Crop nutrition deleted successfully")
                loadCropNutritionList()
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to delete crop nutrition")
            }
        }
    }

    sealed class UiState {
        object Initial : UiState()
        object Loading : UiState()
        data class Success(val message: String) : UiState()
        data class Error(val message: String) : UiState()
    }
}