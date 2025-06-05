package com.example.farmdatapod.logistics.inboundAndOutbound.outboundLogistics.dispatch.data


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

class DispatchViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = DispatchRepository(application)

    // UI State
    private val _uiState = MutableStateFlow<DispatchUiState>(DispatchUiState.Initial)
    val uiState: StateFlow<DispatchUiState> = _uiState

    // Dispatch List
    private val _dispatches = MutableLiveData<List<DispatchEntity>>()
    val dispatches: LiveData<List<DispatchEntity>> = _dispatches

    // Loading State
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // Error State
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    init {
        loadDispatches()
    }

    fun createDispatch(dispatch: DispatchEntity) {
        viewModelScope.launch {
            try {
                _uiState.value = DispatchUiState.Loading

                // Save dispatch locally first
                repository.saveDispatch(dispatch, false)
                    .onSuccess {
                        _uiState.value = DispatchUiState.Success("Dispatch created successfully")

                        // If online, sync the dispatch
                        if (NetworkUtils.isNetworkAvailable(getApplication())) {
                            syncDispatches()
                        }
                    }
                    .onFailure { exception ->
                        _uiState.value = DispatchUiState.Error(exception.message ?: "Error creating dispatch")
                    }
            } catch (e: Exception) {
                _uiState.value = DispatchUiState.Error(e.message ?: "Error creating dispatch")
            }
        }
    }

    fun loadDispatches() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.getAllDispatches()
                    .catch { e ->
                        _error.value = e.message
                    }
                    .collect { dispatchList ->
                        _dispatches.value = dispatchList
                        _isLoading.value = false
                    }
            } catch (e: Exception) {
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }

    fun syncDispatches() {
        viewModelScope.launch {
            try {
                _uiState.value = DispatchUiState.Loading
                repository.performFullSync()
                    .onSuccess {
                        _uiState.value = DispatchUiState.Success("Dispatches synced successfully")
                    }
                    .onFailure { exception ->
                        _uiState.value = DispatchUiState.Error(exception.message ?: "Error syncing dispatches")
                    }
            } catch (e: Exception) {
                _uiState.value = DispatchUiState.Error(e.message ?: "Error syncing dispatches")
            }
        }
    }

    fun deleteDispatch(dispatch: DispatchEntity) {
        viewModelScope.launch {
            try {
                repository.deleteDispatch(dispatch)
                _uiState.value = DispatchUiState.Success("Dispatch deleted successfully")
            } catch (e: Exception) {
                _uiState.value = DispatchUiState.Error(e.message ?: "Error deleting dispatch")
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
                    if (modelClass.isAssignableFrom(DispatchViewModel::class.java)) {
                        return DispatchViewModel(application) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
        }
    }
}

sealed class DispatchUiState {
    object Initial : DispatchUiState()
    object Loading : DispatchUiState()
    data class Success(val message: String) : DispatchUiState()
    data class Error(val message: String) : DispatchUiState()
}