package com.example.farmdatapod.season.landPreparation.data


import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.farmdatapod.utils.NetworkUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LandPreparationViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = LandPreparationRepository(application)

    // UI State
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _success = MutableLiveData<String?>()
    val success: LiveData<String?> = _success

    // Form Data
    private val _landPreparations = MutableStateFlow<List<LandPreparationWithDetails>>(emptyList())
    val landPreparations: StateFlow<List<LandPreparationWithDetails>> = _landPreparations

    // Selected Producer's Land Preparations
    private val _producerLandPreparations = MutableStateFlow<List<LandPreparationWithDetails>>(emptyList())
    val producerLandPreparations: StateFlow<List<LandPreparationWithDetails>> = _producerLandPreparations

    init {
        loadAllLandPreparations()
    }

    private fun loadAllLandPreparations() {
        viewModelScope.launch {
            try {
                repository.getAllLandPreparations().collect { preparations ->
                    _landPreparations.value = preparations
                }
            } catch (e: Exception) {
                _error.value = "Error loading land preparations: ${e.message}"
            }
        }
    }

    fun loadLandPreparationsForProducer(producerId: Int) {
        viewModelScope.launch {
            try {
                repository.getLandPreparationsByProducer(producerId).collect { preparations ->
                    _producerLandPreparations.value = preparations
                }
            } catch (e: Exception) {
                _error.value = "Error loading producer's land preparations: ${e.message}"
            }
        }
    }

    fun saveLandPreparation(
        landPrep: LandPreparationEntity,
        coverCrop: CoverCropEntity?,
        mulching: MulchingEntity?,
        soilAnalysis: SoilAnalysisEntity?
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val isOnline = NetworkUtils.isNetworkAvailable(getApplication())

                val result = repository.saveLandPreparation(
                    landPrep,
                    coverCrop,
                    mulching,
                    soilAnalysis,
                    isOnline
                )

                result.fold(
                    onSuccess = { id ->
                        _success.value = "Land preparation saved successfully"
                        if (!isOnline) {
                            _success.value = "Land preparation saved offline. Will sync when online."
                        }
                    },
                    onFailure = { e ->
                        _error.value = "Error saving land preparation: ${e.message}"
                    }
                )
            } catch (e: Exception) {
                _error.value = "Error saving land preparation: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun syncWithServer() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val result = repository.performFullSync()
                result.fold(
                    onSuccess = { stats ->
                        val message = buildString {
                            append("Sync completed: ")
                            append("${stats.uploadedCount} uploaded, ")
                            append("${stats.downloadedCount} downloaded, ")
                            append("${stats.uploadFailures} failures")
                        }
                        _success.value = message
                    },
                    onFailure = { e ->
                        _error.value = "Sync failed: ${e.message}"
                    }
                )
            } catch (e: Exception) {
                _error.value = "Sync error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getLandPreparationById(id: Long) {
        viewModelScope.launch {
            try {
                val landPrep = repository.getLandPreparationById(id)
                if (landPrep == null) {
                    _error.value = "Land preparation not found"
                }
                // Handle the land preparation data as needed
            } catch (e: Exception) {
                _error.value = "Error loading land preparation: ${e.message}"
            }
        }
    }

    fun deleteLandPreparation(landPrep: LandPreparationEntity) {
        viewModelScope.launch {
            try {
                repository.deleteLandPreparation(landPrep)
                _success.value = "Land preparation deleted successfully"
            } catch (e: Exception) {
                _error.value = "Error deleting land preparation: ${e.message}"
            }
        }
    }

    // Clear messages
    fun clearError() {
        _error.value = null
    }

    fun clearSuccess() {
        _success.value = null
    }
}


