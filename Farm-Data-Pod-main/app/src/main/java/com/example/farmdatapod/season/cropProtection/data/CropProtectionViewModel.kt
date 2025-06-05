package com.example.farmdatapod.season.cropProtection.data




import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.farmdatapod.models.NameOfApplicants
import com.example.farmdatapod.utils.NetworkUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CropProtectionViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CropProtectionRepository(application)

    private val _savingState = MutableStateFlow<SavingState>(SavingState.Idle)
    val savingState: StateFlow<SavingState> = _savingState

    private val _errorState = MutableStateFlow<String?>(null)
    val errorState: StateFlow<String?> = _errorState

    fun getAllCropProtections(): Flow<List<CropProtectionWithApplicants>> =
        repository.getAllCropProtections()

    suspend fun getCropProtectionById(id: Long): Flow<CropProtectionWithApplicants?> =
        repository.getCropProtectionById(id)

    fun saveCropProtection(
        cropProtection: CropProtectionEntity,
        applicants: List<NameOfApplicants>
    ) {
        viewModelScope.launch {
            try {
                _savingState.value = SavingState.Saving
                val isOnline = NetworkUtils.isNetworkAvailable(getApplication())

                repository.saveCropProtection(cropProtection, applicants, isOnline)
                    .onSuccess {
                        _savingState.value = SavingState.Success
                    }
                    .onFailure { error ->
                        _errorState.value = error.message ?: "Unknown error occurred"
                        _savingState.value = SavingState.Error
                    }
            } catch (e: Exception) {
                _errorState.value = e.message ?: "Unknown error occurred"
                _savingState.value = SavingState.Error
            }
        }
    }

    fun updateCropProtection(
        cropProtection: CropProtectionEntity,
        applicants: List<NameOfApplicants>
    ) {
        viewModelScope.launch {
            try {
                _savingState.value = SavingState.Saving
                repository.updateCropProtection(cropProtection, applicants)
                _savingState.value = SavingState.Success
            } catch (e: Exception) {
                _errorState.value = e.message ?: "Unknown error occurred"
                _savingState.value = SavingState.Error
            }
        }
    }

    fun deleteCropProtection(cropProtection: CropProtectionEntity) {
        viewModelScope.launch {
            try {
                repository.deleteCropProtection(cropProtection)
            } catch (e: Exception) {
                _errorState.value = e.message ?: "Error deleting crop protection"
            }
        }
    }

    fun syncWithServer() {
        viewModelScope.launch {
            try {
                repository.performFullSync()
                    .onSuccess { stats ->
                        _errorState.value = if (stats.successful) {
                            "Sync completed: ${stats.uploadedCount} uploaded, ${stats.downloadedCount} downloaded"
                        } else {
                            "Sync completed with some issues: ${stats.uploadFailures} failures"
                        }
                    }
                    .onFailure { error ->
                        _errorState.value = error.message ?: "Sync failed"
                    }
            } catch (e: Exception) {
                _errorState.value = e.message ?: "Error during sync"
            }
        }
    }

    fun clearError() {
        _errorState.value = null
    }

    fun resetSavingState() {
        _savingState.value = SavingState.Idle
    }

    sealed class SavingState {
        object Idle : SavingState()
        object Saving : SavingState()
        object Success : SavingState()
        object Error : SavingState()
    }
}