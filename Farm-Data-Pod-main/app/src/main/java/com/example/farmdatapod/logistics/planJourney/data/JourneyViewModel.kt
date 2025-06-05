package com.example.farmdatapod.logistics.planJourney.data

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.farmdatapod.utils.NetworkUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class JourneyViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = JourneyRepository(application)
    val journeys = repository.getAllJourneys().asLiveData()
    private val _journeyBasicInfo = MutableLiveData<List<JourneyBasicInfo>>()
    val journeyBasicInfo: LiveData<List<JourneyBasicInfo>> = _journeyBasicInfo

    init {
        loadJourneyBasicInfo() // Add this to load basic info on init
    }

    // State Management
    sealed class SyncState {
        object Idle : SyncState()
        object Syncing : SyncState()
        data class Success(val message: String) : SyncState()
        data class Error(val message: String) : SyncState()
    }

    sealed class JourneyInfoState {
        object Loading : JourneyInfoState()
        data class Success(
            val journeyInfo: List<JourneyBasicInfo>
        ) : JourneyInfoState()
        data class Error(val message: String) : JourneyInfoState()
    }

    sealed class JourneyDetailState {
        object Loading : JourneyDetailState()
        data class Success(
            val journeyDetails: List<JourneyStopPointInfo>
        ) : JourneyDetailState()
        data class Error(val message: String) : JourneyDetailState()
    }
    fun loadJourneyBasicInfo() {
        viewModelScope.launch {
            try {
                repository.getJourneyNamesAndIds()
                    .onSuccess { journeyInfo ->
                        _journeyBasicInfo.value = journeyInfo
                    }
                    .onFailure { error ->
                        // Handle error if needed
                        Log.e("JourneyViewModel", "Error loading journey info", error)
                    }
            } catch (e: Exception) {
                Log.e("JourneyViewModel", "Error loading journey basic info", e)
            }
        }
    }
    // StateFlow declarations
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState

    private val _journeyInfoState = MutableStateFlow<JourneyInfoState>(JourneyInfoState.Loading)
    val journeyInfoState: StateFlow<JourneyInfoState> = _journeyInfoState

    private val _journeyDetailState = MutableStateFlow<JourneyDetailState>(JourneyDetailState.Loading)
    val journeyDetailState: StateFlow<JourneyDetailState> = _journeyDetailState

    // Sync management
    private var lastSyncTime = 0L
    private val SYNC_COOLDOWN = 2000L // 2 seconds cooldown
    private var journeyNamesMap: Map<Long, String> = emptyMap()

    // Journey Information Methods
    fun loadJourneyInfo() {
        viewModelScope.launch {
            _journeyInfoState.value = JourneyInfoState.Loading
            try {
                repository.getJourneyNamesAndIds()
                    .onSuccess { journeyInfo ->
                        _journeyInfoState.value = JourneyInfoState.Success(journeyInfo)
                    }
                    .onFailure { error ->
                        _journeyInfoState.value = JourneyInfoState.Error(
                            error.message ?: "Failed to load journey information"
                        )
                    }
            } catch (e: Exception) {
                _journeyInfoState.value = JourneyInfoState.Error(
                    e.message ?: "Error loading journey information"
                )
            }
        }
    }

    fun loadJourneyDetails() {
        viewModelScope.launch {
            _journeyDetailState.value = JourneyDetailState.Loading
            try {
                repository.getJourneysWithStopPointInfo()
                    .onSuccess { details ->
                        _journeyDetailState.value = JourneyDetailState.Success(details)
                    }
                    .onFailure { error ->
                        _journeyDetailState.value = JourneyDetailState.Error(
                            error.message ?: "Failed to load journey details"
                        )
                    }
            } catch (e: Exception) {
                _journeyDetailState.value = JourneyDetailState.Error(
                    e.message ?: "Error loading journey details"
                )
            }
        }
    }

    fun loadJourneyNamesMap() {
        viewModelScope.launch {
            try {
                repository.getJourneyNamesMap()
                    .onSuccess { map ->
                        journeyNamesMap = map
                    }
            } catch (e: Exception) {
                // Handle exception if needed
            }
        }
    }

    fun getJourneyName(journeyId: Long): String = journeyNamesMap[journeyId] ?: "Unknown Journey"

    // Sync Operations
    suspend fun syncWithServer() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastSyncTime < SYNC_COOLDOWN) {
            _syncState.value = SyncState.Error("Please wait before syncing again")
            return
        }

        if (_syncState.value is SyncState.Syncing) return

        lastSyncTime = currentTime
        _syncState.value = SyncState.Syncing

        try {
            repository.performFullSync()
                .onSuccess { stats ->
                    if (stats.successful) {
                        _syncState.value = SyncState.Success(
                            "Sync completed: ${stats.uploadedCount} uploaded, ${stats.downloadedCount} downloaded"
                        )
                        // Refresh journey information after successful sync
                        loadJourneyInfo()
                        loadJourneyDetails()
                        loadJourneyNamesMap()
                    } else {
                        _syncState.value = SyncState.Error(
                            "Sync completed with issues: ${stats.uploadFailures} failures"
                        )
                    }
                }
                .onFailure { error ->
                    _syncState.value = SyncState.Error(error.message ?: "Sync failed")
                }
        } catch (e: Exception) {
            _syncState.value = SyncState.Error(e.message ?: "Error during sync")
        }
    }

    // Journey CRUD Operations
    fun saveJourney(journey: JourneyEntity, stopPoints: List<StopPointEntity>) {
        if (_syncState.value is SyncState.Syncing) return

        viewModelScope.launch {
            try {
                _syncState.value = SyncState.Syncing
                val isOnline = NetworkUtils.isNetworkAvailable(getApplication())

                repository.saveJourney(journey, stopPoints, isOnline)
                    .onSuccess {
                        _syncState.value = SyncState.Success("Journey saved successfully")
                        // Refresh journey information after successful save
                        loadJourneyInfo()
                        loadJourneyDetails()
                        loadJourneyNamesMap()
                    }
                    .onFailure { error ->
                        _syncState.value = SyncState.Error(error.message ?: "Failed to save journey")
                    }
            } catch (e: Exception) {
                _syncState.value = SyncState.Error(e.message ?: "Error saving journey")
            }
        }
    }

    fun updateJourney(journeyWithStops: JourneyWithStopPoints) {
        viewModelScope.launch {
            try {
                _syncState.value = SyncState.Syncing
                repository.updateJourney(journeyWithStops)
                _syncState.value = SyncState.Success("Journey updated successfully")
                // Refresh journey information after successful update
                loadJourneyInfo()
                loadJourneyDetails()
                loadJourneyNamesMap()
            } catch (e: Exception) {
                _syncState.value = SyncState.Error(e.message ?: "Failed to update journey")
            }
        }
    }

    fun deleteJourney(journey: JourneyEntity) {
        viewModelScope.launch {
            try {
                _syncState.value = SyncState.Syncing
                repository.deleteJourney(journey)
                _syncState.value = SyncState.Success("Journey deleted successfully")
                // Refresh journey information after successful deletion
                loadJourneyInfo()
                loadJourneyDetails()
                loadJourneyNamesMap()
            } catch (e: Exception) {
                _syncState.value = SyncState.Error(e.message ?: "Error deleting journey")
            }
        }
    }
    // Add this function to your existing JourneyViewModel class
    fun loadJourneyDetailsForJourney(journeyId: Long) {
        viewModelScope.launch {
            _journeyDetailState.value = JourneyDetailState.Loading
            try {
                repository.getJourneysWithStopPointInfo()
                    .onSuccess { allDetails ->
                        // Filter stop points for this specific journey
                        val journeyStopPoints = allDetails.filter { detail ->
                            detail.journey_id == journeyId
                        }

                        if (journeyStopPoints.isEmpty()) {
                            _journeyDetailState.value = JourneyDetailState.Error("No stop points found for this journey")
                        } else {
                            _journeyDetailState.value = JourneyDetailState.Success(journeyStopPoints)
                        }
                    }
                    .onFailure { error ->
                        _journeyDetailState.value = JourneyDetailState.Error(
                            error.message ?: "Failed to load journey details"
                        )
                    }
            } catch (e: Exception) {
                _journeyDetailState.value = JourneyDetailState.Error(
                    e.message ?: "Error loading journey details"
                )
            }
        }
    }

    // Optional: Add this helper function to check if a stop point belongs to a journey
    fun isStopPointForJourney(journeyId: Long, stopPointId: Long): Boolean {
        val currentDetails = (_journeyDetailState.value as? JourneyDetailState.Success)?.journeyDetails
        return currentDetails?.any {
            it.journey_id == journeyId && it.stop_point_id == stopPointId
        } ?: false
    }

    // Optional: Add this helper function to get stop points for a specific journey
    fun getStopPointsForJourney(journeyId: Long): List<JourneyStopPointInfo> {
        return (_journeyDetailState.value as? JourneyDetailState.Success)?.journeyDetails
            ?.filter { it.journey_id == journeyId }
            ?: emptyList()
    }

    // Query Operations
    fun getAllJourneys(): Flow<List<JourneyWithStopPoints>> = repository.getAllJourneys()

    suspend fun getJourneyById(id: Long): JourneyWithStopPoints? = repository.getJourneyById(id)

    // State Management
    fun resetState() {
        _syncState.value = SyncState.Idle
        _journeyInfoState.value = JourneyInfoState.Loading
        _journeyDetailState.value = JourneyDetailState.Loading
    }

    // Factory
    companion object {
        fun factory(application: Application): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(JourneyViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return JourneyViewModel(application) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}