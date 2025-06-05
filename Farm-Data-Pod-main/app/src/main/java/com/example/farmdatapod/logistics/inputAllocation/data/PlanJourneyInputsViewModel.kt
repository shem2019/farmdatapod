package com.example.farmdatapod.logistics.inputAllocation.data

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.farmdatapod.utils.NetworkUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class PlanJourneyInputsViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "PlanJourneyInputsVM"
    private val repository = PlanJourneyInputsRepository(application)

    // Jobs for cancellation
    private var filterJob: Job? = null
    private var loadJob: Job? = null
    private var syncJob: Job? = null

    // UI State
    private val _uiState = MutableStateFlow<PlanJourneyInputsUiState>(PlanJourneyInputsUiState.Initial)
    val uiState: StateFlow<PlanJourneyInputsUiState> = _uiState

    // Available selections
    private val _availableJourneys = MutableLiveData<List<String>>()
    val availableJourneys: LiveData<List<String>> = _availableJourneys

    private val _availableStopPoints = MutableLiveData<List<String>>()
    val availableStopPoints: LiveData<List<String>> = _availableStopPoints

    // Data lists
    private val _planJourneyInputs = MutableLiveData<List<PlanJourneyInputsEntity>>()
    val planJourneyInputs: LiveData<List<PlanJourneyInputsEntity>> = _planJourneyInputs

    // Loading and Error states
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    init {
        loadPlanJourneyInputs()
    }

    fun loadJourneysAndStopPoints() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                repository.getAllPlanJourneyInputs()
                    .catch { e ->
                        handleException(e, "Error loading journeys and stop points")
                    }
                    .collect { inputs ->
                        try {
                            ensureActive()
                            _availableJourneys.value = inputs.map { it.journey_id }.distinct()
                            _availableStopPoints.value = inputs.map { it.stop_point_id }.distinct()
                        } catch (e: CancellationException) {
                            Log.d(TAG, "Load operation cancelled during processing")
                            throw e
                        } catch (e: Exception) {
                            handleException(e, "Error processing inputs")
                        }
                    }
            } catch (e: CancellationException) {
                Log.d(TAG, "Load operation cancelled - this is normal during navigation")
                throw e
            } catch (e: Exception) {
                handleException(e, "Error loading data")
            } finally {
                _isLoading.value = false
                loadJob = null
            }
        }
    }

    fun createPlanJourneyInput(planJourneyInput: PlanJourneyInputsEntity) {
        viewModelScope.launch {
            try {
                _uiState.value = PlanJourneyInputsUiState.Loading
                _error.value = null

                repository.savePlanJourneyInput(planJourneyInput, false)
                    .onSuccess {
                        _uiState.value = PlanJourneyInputsUiState.Success("Plan journey input created successfully")
                        if (NetworkUtils.isNetworkAvailable(getApplication())) {
                            syncPlanJourneyInputs()
                        }
                    }
                    .onFailure { exception ->
                        handleException(exception, "Error creating plan journey input")
                    }
            } catch (e: CancellationException) {
                Log.d(TAG, "Create operation cancelled - this is normal during navigation")
                throw e
            } catch (e: Exception) {
                handleException(e, "Error creating input")
            }
        }
    }

    fun loadPlanJourneyInputs() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                repository.getAllPlanJourneyInputs()
                    .catch { e ->
                        handleException(e, "Error loading plan journey inputs")
                    }
                    .collect { inputs ->
                        try {
                            ensureActive()
                            _planJourneyInputs.value = inputs
                        } catch (e: CancellationException) {
                            Log.d(TAG, "Load operation cancelled during processing")
                            throw e
                        } catch (e: Exception) {
                            handleException(e, "Error processing inputs")
                        }
                    }
            } catch (e: CancellationException) {
                Log.d(TAG, "Load operation cancelled - this is normal during navigation")
                throw e
            } catch (e: Exception) {
                handleException(e, "Error loading inputs")
            } finally {
                _isLoading.value = false
                loadJob = null
            }
        }
    }

    fun filterInputs(journeyId: String? = null, stopPointId: String? = null) {
        filterJob?.cancel()
        filterJob = viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                repository.getAllPlanJourneyInputs()
                    .catch { e ->
                        handleException(e, "Error getting inputs for filtering")
                    }
                    .collect { allInputs ->
                        try {
                            ensureActive()
                            Log.d(TAG, "Total inputs before filter: ${allInputs.size}")
                            Log.d(TAG, "Filtering with journeyId: $journeyId, stopPointId: $stopPointId")

                            val filtered = allInputs.filter { input ->
                                ensureActive()
                                Log.d(TAG, "Checking input - Journey: ${input.journey_id}, StopPoint: ${input.stop_point_id}")

                                val journeyMatch = journeyId?.let {
                                    input.journey_id.toString() == it
                                } ?: true

                                val stopPointMatch = stopPointId?.let {
                                    input.stop_point_id.toString() == it
                                } ?: true

                                Log.d(TAG, "Match results - Journey: $journeyMatch, StopPoint: $stopPointMatch")
                                journeyMatch && stopPointMatch
                            }

                            Log.d(TAG, "Filtered inputs size: ${filtered.size}")

                            if (filtered.isEmpty() && allInputs.isNotEmpty()) {
                                _error.value = "No inputs found for Journey: $journeyId, Stop Point: $stopPointId"
                            }

                            _planJourneyInputs.value = filtered
                        } catch (e: CancellationException) {
                            Log.d(TAG, "Filter operation cancelled during processing")
                            throw e
                        } catch (e: Exception) {
                            handleException(e, "Error processing filtered inputs")
                        }
                    }
            } catch (e: CancellationException) {
                Log.d(TAG, "Filter operation cancelled - this is normal during navigation")
                throw e
            } catch (e: Exception) {
                handleException(e, "Error filtering inputs")
            } finally {
                _isLoading.value = false
                filterJob = null
            }
        }
    }

    fun syncPlanJourneyInputs() {
        syncJob?.cancel()
        syncJob = viewModelScope.launch {
            try {
                _uiState.value = PlanJourneyInputsUiState.Loading
                _error.value = null

                repository.performFullSync()
                    .onSuccess {
                        _uiState.value = PlanJourneyInputsUiState.Success("Plan journey inputs synced successfully")
                        loadPlanJourneyInputs() // Refresh data after successful sync
                    }
                    .onFailure { exception ->
                        handleException(exception, "Error syncing plan journey inputs")
                    }
            } catch (e: CancellationException) {
                Log.d(TAG, "Sync operation cancelled - this is normal during navigation")
                throw e
            } catch (e: Exception) {
                handleException(e, "Error during sync")
            } finally {
                syncJob = null
            }
        }
    }

    fun deletePlanJourneyInput(planJourneyInput: PlanJourneyInputsEntity) {
        viewModelScope.launch {
            try {
                _uiState.value = PlanJourneyInputsUiState.Loading
                _error.value = null

                repository.deletePlanJourneyInput(planJourneyInput)
                _uiState.value = PlanJourneyInputsUiState.Success("Plan journey input deleted successfully")
                loadPlanJourneyInputs() // Refresh data after deletion
            } catch (e: CancellationException) {
                Log.d(TAG, "Delete operation cancelled - this is normal during navigation")
                throw e
            } catch (e: Exception) {
                handleException(e, "Error deleting plan journey input")
            }
        }
    }

    private fun handleException(e: Throwable, baseMessage: String) {
        when (e) {
            is CancellationException -> {
                Log.d(TAG, "Operation cancelled - this is normal during navigation")
                throw e
            }
            else -> {
                val errorMessage = e.message ?: baseMessage
                Log.e(TAG, errorMessage, e)
                _error.value = errorMessage
                _uiState.value = PlanJourneyInputsUiState.Error(errorMessage)
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    override fun onCleared() {
        super.onCleared()
        filterJob?.cancel()
        loadJob?.cancel()
        syncJob?.cancel()
        filterJob = null
        loadJob = null
        syncJob = null
    }

    companion object {
        fun factory(application: Application): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(PlanJourneyInputsViewModel::class.java)) {
                        return PlanJourneyInputsViewModel(application) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
        }
    }
}

sealed class PlanJourneyInputsUiState {
    object Initial : PlanJourneyInputsUiState()
    object Loading : PlanJourneyInputsUiState()
    data class Success(val message: String) : PlanJourneyInputsUiState()
    data class Error(val message: String) : PlanJourneyInputsUiState()
}