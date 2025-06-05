package com.example.farmdatapod.logistics.equipments.data

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

class EquipmentViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = EquipmentRepository(application)

    // UI State
    private val _uiState = MutableStateFlow<EquipmentUiState>(EquipmentUiState.Initial)
    val uiState: StateFlow<EquipmentUiState> = _uiState

    // Equipment List
    private val _equipment = MutableLiveData<List<EquipmentEntity>>()
    val equipment: LiveData<List<EquipmentEntity>> = _equipment

    // Loading State
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // Error State
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    init {
        loadEquipment()
    }

    fun createEquipment(equipment: EquipmentEntity) {
        viewModelScope.launch {
            try {
                _uiState.value = EquipmentUiState.Loading

                // Save equipment locally first
                repository.saveEquipment(equipment, false)
                    .onSuccess {
                        _uiState.value = EquipmentUiState.Success("Equipment created successfully")

                        // If online, sync the equipment
                        if (NetworkUtils.isNetworkAvailable(getApplication())) {
                            syncEquipment()
                        }
                    }
                    .onFailure { exception ->
                        _uiState.value = EquipmentUiState.Error(exception.message ?: "Error creating equipment")
                    }
            } catch (e: Exception) {
                _uiState.value = EquipmentUiState.Error(e.message ?: "Error creating equipment")
            }
        }
    }

    fun loadEquipment() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.getAllEquipment()
                    .catch { e ->
                        _error.value = e.message
                    }
                    .collect { equipmentList ->
                        _equipment.value = equipmentList
                        _isLoading.value = false
                    }
            } catch (e: Exception) {
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }

    fun syncEquipment() {
        viewModelScope.launch {
            try {
                _uiState.value = EquipmentUiState.Loading
                repository.performFullSync()
                    .onSuccess {
                        _uiState.value = EquipmentUiState.Success("Equipment synced successfully")
                    }
                    .onFailure { exception ->
                        _uiState.value = EquipmentUiState.Error(exception.message ?: "Error syncing equipment")
                    }
            } catch (e: Exception) {
                _uiState.value = EquipmentUiState.Error(e.message ?: "Error syncing equipment")
            }
        }
    }

    fun deleteEquipment(equipment: EquipmentEntity) {
        viewModelScope.launch {
            try {
                repository.deleteEquipment(equipment)
                _uiState.value = EquipmentUiState.Success("Equipment deleted successfully")
            } catch (e: Exception) {
                _uiState.value = EquipmentUiState.Error(e.message ?: "Error deleting equipment")
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
                    if (modelClass.isAssignableFrom(EquipmentViewModel::class.java)) {
                        return EquipmentViewModel(application) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
        }
    }
}

sealed class EquipmentUiState {
    object Initial : EquipmentUiState()
    object Loading : EquipmentUiState()
    data class Success(val message: String) : EquipmentUiState()
    data class Error(val message: String) : EquipmentUiState()
}