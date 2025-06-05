package com.example.farmdatapod.logistics.inboundAndOutbound.outboundLogistics.loadedInput.data


import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.farmdatapod.utils.NetworkUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException


class LoadingInputViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = LoadingInputRepository(application)
    private var filterJob: Job? = null


    // UI State
    private val _uiState = MutableStateFlow<LoadingInputUiState>(LoadingInputUiState.Initial)
    val uiState: StateFlow<LoadingInputUiState> = _uiState

    // Journey and StopPoint filters
    private val _availableJourneys = MutableLiveData<List<String>>()
    val availableJourneys: LiveData<List<String>> = _availableJourneys

    private val _availableStopPoints = MutableLiveData<List<String>>()
    val availableStopPoints: LiveData<List<String>> = _availableStopPoints

    // Loading Inputs List
    private val _loadingInputs = MutableLiveData<List<LoadingInputEntity>>()
    val loadingInputs: LiveData<List<LoadingInputEntity>> = _loadingInputs

    // Loading State
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // Error State
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    init {
        loadLoadingInputs()
    }

    fun loadJourneysAndStopPoints() {
        viewModelScope.launch {
            try {
                repository.getAllLoadingInputs()
                    .collect { inputs ->
                        // Extract unique journeys and stop points
                        _availableJourneys.value = inputs.map { it.journey_id.toString() }.distinct()
                        _availableStopPoints.value = inputs.map { it.stop_point_id.toString() }.distinct()
                    }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun filterInputs(journeyId: Int? = null, stopPointId: Int? = null) {
        // Cancel any existing filter operation
        filterJob?.cancel()

        filterJob = viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                when {
                    journeyId != null -> {
                        repository.getLoadingInputsByJourneyId(journeyId)
                            .catch { e ->
                                _error.value = e.message
                                _loadingInputs.value = emptyList()
                            }
                            .collect { filteredInputs ->
                                val finalFiltered = filteredInputs.filter { input ->
                                    stopPointId == null || input.stop_point_id == stopPointId
                                }
                                _loadingInputs.value = finalFiltered
                            }
                    }
                    else -> {
                        repository.getAllLoadingInputs()
                            .catch { e ->
                                _error.value = e.message
                                _loadingInputs.value = emptyList()
                            }
                            .collect { allInputs ->
                                val finalFiltered = allInputs.filter { input ->
                                    stopPointId == null || input.stop_point_id == stopPointId
                                }
                                _loadingInputs.value = finalFiltered
                            }
                    }
                }
            } catch (e: CancellationException) {
                // Handle cancellation gracefully
                Log.d("LoadingInputViewModel", "Filter operation cancelled")
                throw e  // Propagate cancellation
            } catch (e: Exception) {
                _error.value = e.message
                _loadingInputs.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        filterJob?.cancel()
        filterJob = null
    }

    fun saveLoadingInput(loadingInput: LoadingInputEntity) {
        viewModelScope.launch {
            try {
                _uiState.value = LoadingInputUiState.Loading

                // Save loading input locally first
                repository.saveLoadingInput(loadingInput, false)
                    .onSuccess {
                        _uiState.value = LoadingInputUiState.Success("Loading input saved successfully")

                        // If online, sync the loading input
                        if (NetworkUtils.isNetworkAvailable(getApplication())) {
                            syncLoadingInputs()
                        }
                    }
                    .onFailure { exception ->
                        _uiState.value = LoadingInputUiState.Error(exception.message ?: "Error saving loading input")
                    }
            } catch (e: Exception) {
                _uiState.value = LoadingInputUiState.Error(e.message ?: "Error saving loading input")
            }
        }
    }

    fun loadLoadingInputs() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.getAllLoadingInputs()
                    .catch { e ->
                        _error.value = e.message
                    }
                    .collect { loadingInputsList ->
                        _loadingInputs.value = loadingInputsList
                        _isLoading.value = false
                    }
            } catch (e: Exception) {
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }

    fun syncLoadingInputs() {
        viewModelScope.launch {
            try {
                _uiState.value = LoadingInputUiState.Loading
                repository.performFullSync()
                    .onSuccess {
                        _uiState.value = LoadingInputUiState.Success("Loading inputs synced successfully")
                    }
                    .onFailure { exception ->
                        _uiState.value = LoadingInputUiState.Error(exception.message ?: "Error syncing loading inputs")
                    }
            } catch (e: Exception) {
                _uiState.value = LoadingInputUiState.Error(e.message ?: "Error syncing loading inputs")
            }
        }
    }

    fun deleteLoadingInput(loadingInput: LoadingInputEntity) {
        viewModelScope.launch {
            try {
                repository.deleteLoadingInput(loadingInput)
                _uiState.value = LoadingInputUiState.Success("Loading input deleted successfully")
            } catch (e: Exception) {
                _uiState.value = LoadingInputUiState.Error(e.message ?: "Error deleting loading input")
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    companion object {
        fun factory(application: Application): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(LoadingInputViewModel::class.java)) {
                        return LoadingInputViewModel(application) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
        }
    }
}