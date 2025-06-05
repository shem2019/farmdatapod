package com.example.farmdatapod.logistics.inputTransfer.data

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

sealed class InputTransferUiState {
    object Initial : InputTransferUiState()
    object Loading : InputTransferUiState()
    data class Success(val message: String) : InputTransferUiState()
    data class Error(val message: String) : InputTransferUiState()
}

class InputTransferViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = InputTransferRepository(application)

    private val _uiState = MutableStateFlow<InputTransferUiState>(InputTransferUiState.Initial)
    val uiState: StateFlow<InputTransferUiState> = _uiState

    private val _inputTransfers = MutableLiveData<List<InputTransferEntity>>()
    val inputTransfers: LiveData<List<InputTransferEntity>> = _inputTransfers

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    init {
        loadInputTransfers()
    }

    fun createInputTransfer(inputTransfer: InputTransferEntity) {
        viewModelScope.launch {
            try {
                _uiState.value = InputTransferUiState.Loading

                repository.saveInputTransfer(inputTransfer, false)
                    .onSuccess {
                        _uiState.value = InputTransferUiState.Success("Input transfer created successfully")

                        if (NetworkUtils.isNetworkAvailable(getApplication())) {
                            syncInputTransfers()
                        }
                    }
                    .onFailure { exception ->
                        _uiState.value = InputTransferUiState.Error(exception.message ?: "Error creating input transfer")
                    }
            } catch (e: Exception) {
                _uiState.value = InputTransferUiState.Error(e.message ?: "Error creating input transfer")
            }
        }
    }

    fun loadInputTransfers() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.getAllInputTransfers()
                    .catch { e ->
                        _error.value = e.message
                    }
                    .collect { inputTransfersList ->
                        _inputTransfers.value = inputTransfersList
                        _isLoading.value = false
                    }
            } catch (e: Exception) {
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }

    fun syncInputTransfers() {
        viewModelScope.launch {
            try {
                _uiState.value = InputTransferUiState.Loading
                repository.performFullSync()
                    .onSuccess {
                        _uiState.value = InputTransferUiState.Success("Input transfers synced successfully")
                    }
                    .onFailure { exception ->
                        _uiState.value = InputTransferUiState.Error(exception.message ?: "Error syncing input transfers")
                    }
            } catch (e: Exception) {
                _uiState.value = InputTransferUiState.Error(e.message ?: "Error syncing input transfers")
            }
        }
    }

    fun deleteInputTransfer(inputTransfer: InputTransferEntity) {
        viewModelScope.launch {
            try {
                repository.deleteInputTransfer(inputTransfer)
                _uiState.value = InputTransferUiState.Success("Input transfer deleted successfully")
            } catch (e: Exception) {
                _uiState.value = InputTransferUiState.Error(e.message ?: "Error deleting input transfer")
            }
        }
    }

    fun getInputTransfersByHub(hubId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.getInputTransfersByHub(hubId)
                    .catch { e ->
                        _error.value = e.message
                    }
                    .collect { transfersList ->
                        _inputTransfers.value = transfersList
                        _isLoading.value = false
                    }
            } catch (e: Exception) {
                _error.value = e.message
                _isLoading.value = false
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
                    if (modelClass.isAssignableFrom(InputTransferViewModel::class.java)) {
                        return InputTransferViewModel(application) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
        }
    }
}