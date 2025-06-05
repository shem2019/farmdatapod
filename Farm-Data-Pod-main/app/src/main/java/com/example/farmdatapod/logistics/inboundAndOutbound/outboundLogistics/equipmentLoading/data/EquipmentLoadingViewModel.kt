package com.example.farmdatapod.logistics.inboundAndOutbound.outboundLogistics.equipmentLoading.data


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

class EquipmentLoadingViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = EquipmentLoadingRepository(application)

    // UI State
    private val _uiState = MutableStateFlow<EquipmentLoadingUiState>(EquipmentLoadingUiState.Initial)
    val uiState: StateFlow<EquipmentLoadingUiState> = _uiState

    // Equipment Loading List
    private val _equipmentLoadings = MutableLiveData<List<EquipmentLoadingEntity>>()
    val equipmentLoadings: LiveData<List<EquipmentLoadingEntity>> = _equipmentLoadings

    // Loading State
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // Error State
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    init {
        loadEquipmentLoadings()
    }

    fun createEquipmentLoading(equipmentLoading: EquipmentLoadingEntity) {
        viewModelScope.launch {
            try {
                _uiState.value = EquipmentLoadingUiState.Loading

                // Save equipment loading locally first
                repository.saveEquipmentLoading(equipmentLoading, false)
                    .onSuccess {
                        _uiState.value = EquipmentLoadingUiState.Success("Equipment loading created successfully")

                        // If online, sync the equipment loading
                        if (NetworkUtils.isNetworkAvailable(getApplication())) {
                            syncEquipmentLoadings()
                        }
                    }
                    .onFailure { exception ->
                        _uiState.value = EquipmentLoadingUiState.Error(exception.message ?: "Error creating equipment loading")
                    }
            } catch (e: Exception) {
                _uiState.value = EquipmentLoadingUiState.Error(e.message ?: "Error creating equipment loading")
            }
        }
    }

    fun loadEquipmentLoadings() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.getAllEquipmentLoadings()
                    .catch { e ->
                        _error.value = e.message
                    }
                    .collect { loadingList ->
                        _equipmentLoadings.value = loadingList
                        _isLoading.value = false
                    }
            } catch (e: Exception) {
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }

    fun getEquipmentLoadingsByJourneyId(journeyId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.getEquipmentLoadingsByJourneyId(journeyId)
                    .catch { e ->
                        _error.value = e.message
                    }
                    .collect { loadingList ->
                        _equipmentLoadings.value = loadingList
                        _isLoading.value = false
                    }
            } catch (e: Exception) {
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }

    fun syncEquipmentLoadings() {
        viewModelScope.launch {
            try {
                _uiState.value = EquipmentLoadingUiState.Loading
                repository.performFullSync()
                    .onSuccess {
                        _uiState.value = EquipmentLoadingUiState.Success("Equipment loadings synced successfully")
                    }
                    .onFailure { exception ->
                        _uiState.value = EquipmentLoadingUiState.Error(exception.message ?: "Error syncing equipment loadings")
                    }
            } catch (e: Exception) {
                _uiState.value = EquipmentLoadingUiState.Error(e.message ?: "Error syncing equipment loadings")
            }
        }
    }

    fun deleteEquipmentLoading(equipmentLoading: EquipmentLoadingEntity) {
        viewModelScope.launch {
            try {
                repository.deleteEquipmentLoading(equipmentLoading)
                _uiState.value = EquipmentLoadingUiState.Success("Equipment loading deleted successfully")
            } catch (e: Exception) {
                _uiState.value = EquipmentLoadingUiState.Error(e.message ?: "Error deleting equipment loading")
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
                    if (modelClass.isAssignableFrom(EquipmentLoadingViewModel::class.java)) {
                        return EquipmentLoadingViewModel(application) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
        }
    }
}

sealed class EquipmentLoadingUiState {
    object Initial : EquipmentLoadingUiState()
    object Loading : EquipmentLoadingUiState()
    data class Success(val message: String) : EquipmentLoadingUiState()
    data class Error(val message: String) : EquipmentLoadingUiState()
}