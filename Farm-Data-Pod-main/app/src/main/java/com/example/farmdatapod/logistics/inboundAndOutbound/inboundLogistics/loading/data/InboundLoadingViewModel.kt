package com.example.farmdatapod.logistics.inboundAndOutbound.inboundLogistics.loading.data


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

class InboundLoadingViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = InboundLoadingRepository(application)

    // UI State
    private val _uiState = MutableStateFlow<InboundLoadingUiState>(InboundLoadingUiState.Initial)
    val uiState: StateFlow<InboundLoadingUiState> = _uiState

    // Loading List
    private val _loadings = MutableLiveData<List<InboundLoadingEntity>>()
    val loadings: LiveData<List<InboundLoadingEntity>> = _loadings

    // Loading State
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // Error State
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    init {
        loadInboundLoadings()
    }

    fun createLoading(loading: InboundLoadingEntity) {
        viewModelScope.launch {
            try {
                _uiState.value = InboundLoadingUiState.Loading

                // Save loading locally first
                repository.saveLoading(loading, false)
                    .onSuccess {
                        _uiState.value = InboundLoadingUiState.Success("Loading created successfully")

                        // If online, sync the loading
                        if (NetworkUtils.isNetworkAvailable(getApplication())) {
                            syncLoadings()
                        }
                    }
                    .onFailure { exception ->
                        _uiState.value = InboundLoadingUiState.Error(exception.message ?: "Error creating loading")
                    }
            } catch (e: Exception) {
                _uiState.value = InboundLoadingUiState.Error(e.message ?: "Error creating loading")
            }
        }
    }

    fun loadInboundLoadings() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.getAllLoadings()
                    .catch { e ->
                        _error.value = e.message
                    }
                    .collect { loadingList ->
                        _loadings.value = loadingList
                        _isLoading.value = false
                    }
            } catch (e: Exception) {
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }

    fun syncLoadings() {
        viewModelScope.launch {
            try {
                _uiState.value = InboundLoadingUiState.Loading
                repository.performFullSync()
                    .onSuccess {
                        _uiState.value = InboundLoadingUiState.Success("Loadings synced successfully")
                    }
                    .onFailure { exception ->
                        _uiState.value = InboundLoadingUiState.Error(exception.message ?: "Error syncing loadings")
                    }
            } catch (e: Exception) {
                _uiState.value = InboundLoadingUiState.Error(e.message ?: "Error syncing loadings")
            }
        }
    }

    fun deleteLoading(loading: InboundLoadingEntity) {
        viewModelScope.launch {
            try {
                repository.deleteLoading(loading)
                _uiState.value = InboundLoadingUiState.Success("Loading deleted successfully")
            } catch (e: Exception) {
                _uiState.value = InboundLoadingUiState.Error(e.message ?: "Error deleting loading")
            }
        }
    }

    fun updateLoading(loading: InboundLoadingEntity) {
        viewModelScope.launch {
            try {
                _uiState.value = InboundLoadingUiState.Loading
                repository.updateLoading(loading)
                _uiState.value = InboundLoadingUiState.Success("Loading updated successfully")
            } catch (e: Exception) {
                _uiState.value = InboundLoadingUiState.Error(e.message ?: "Error updating loading")
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
                    if (modelClass.isAssignableFrom(InboundLoadingViewModel::class.java)) {
                        return InboundLoadingViewModel(application) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
        }
    }
}

sealed class InboundLoadingUiState {
    object Initial : InboundLoadingUiState()
    object Loading : InboundLoadingUiState()
    data class Success(val message: String) : InboundLoadingUiState()
    data class Error(val message: String) : InboundLoadingUiState()
}