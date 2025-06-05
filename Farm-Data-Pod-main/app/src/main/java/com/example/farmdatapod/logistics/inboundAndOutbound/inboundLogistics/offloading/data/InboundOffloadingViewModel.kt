package com.example.farmdatapod.logistics.inboundAndOutbound.inboundLogistics.offloading.data


import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.farmdatapod.utils.NetworkUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class InboundOffloadingViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = InboundOffloadingRepository(application)

    // UI State
    private val _uiState = MutableStateFlow<InboundOffloadingUiState>(InboundOffloadingUiState.Initial)
    val uiState: StateFlow<InboundOffloadingUiState> = _uiState

    // Offloading List
    private val _offloadings = MutableLiveData<List<InboundOffloadingEntity>>()
    val offloadings: LiveData<List<InboundOffloadingEntity>> = _offloadings

    // Loading State
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // Error State
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    init {
        loadOffloadings()
    }

    fun createOffloading(offloading: InboundOffloadingEntity) {
        viewModelScope.launch {
            try {
                _uiState.value = InboundOffloadingUiState.Loading

                // Save offloading locally first
                repository.saveOffloading(offloading, false)
                    .onSuccess {
                        _uiState.value = InboundOffloadingUiState.Success("Offloading created successfully")

                        // If online, sync the offloading
                        if (NetworkUtils.isNetworkAvailable(getApplication())) {
                            syncOffloadings()
                        }
                    }
                    .onFailure { exception ->
                        _uiState.value = InboundOffloadingUiState.Error(exception.message ?: "Error creating offloading")
                    }
            } catch (e: Exception) {
                _uiState.value = InboundOffloadingUiState.Error(e.message ?: "Error creating offloading")
            }
        }
    }

    fun loadOffloadings() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.getAllOffloadings()
                    .catch { e ->
                        _error.value = e.message
                    }
                    .collect { offloadingList ->
                        _offloadings.value = offloadingList
                        _isLoading.value = false
                    }
            } catch (e: Exception) {
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }

    fun syncOffloadings() {
        viewModelScope.launch {
            try {
                _uiState.value = InboundOffloadingUiState.Loading
                repository.performFullSync()
                    .onSuccess {
                        _uiState.value = InboundOffloadingUiState.Success("Offloadings synced successfully")
                    }
                    .onFailure { exception ->
                        _uiState.value = InboundOffloadingUiState.Error(exception.message ?: "Error syncing offloadings")
                    }
            } catch (e: Exception) {
                _uiState.value = InboundOffloadingUiState.Error(e.message ?: "Error syncing offloadings")
            }
        }
    }

    fun deleteOffloading(offloading: InboundOffloadingEntity) {
        viewModelScope.launch {
            try {
                repository.deleteOffloading(offloading)
                _uiState.value = InboundOffloadingUiState.Success("Offloading deleted successfully")
            } catch (e: Exception) {
                _uiState.value = InboundOffloadingUiState.Error(e.message ?: "Error deleting offloading")
            }
        }
    }

    fun updateOffloading(offloading: InboundOffloadingEntity) {
        viewModelScope.launch {
            try {
                _uiState.value = InboundOffloadingUiState.Loading
                repository.updateOffloading(offloading)
                _uiState.value = InboundOffloadingUiState.Success("Offloading updated successfully")
            } catch (e: Exception) {
                _uiState.value = InboundOffloadingUiState.Error(e.message ?: "Error updating offloading")
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
                    if (modelClass.isAssignableFrom(InboundOffloadingViewModel::class.java)) {
                        return InboundOffloadingViewModel(application) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
        }
    }
}

sealed class InboundOffloadingUiState {
    object Initial : InboundOffloadingUiState()
    object Loading : InboundOffloadingUiState()
    data class Success(val message: String) : InboundOffloadingUiState()
    data class Error(val message: String) : InboundOffloadingUiState()
}