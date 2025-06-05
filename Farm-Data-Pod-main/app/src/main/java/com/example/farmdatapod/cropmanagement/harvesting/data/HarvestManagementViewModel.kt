package com.example.farmdatapod.cropmanagement.harvesting.data


import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.farmdatapod.season.harvest.data.HarvestPlanning
import com.example.farmdatapod.season.harvest.data.HarvestPlanningBuyer
import com.example.farmdatapod.season.harvest.data.HarvestPlanningWithBuyers
import com.example.farmdatapod.utils.NetworkUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HarvestManagementViewModel(
    application: Application,
    private val repository: HarvestManagementRepository
) : AndroidViewModel(application) {

    private val _savingState = MutableStateFlow<SavingState>(SavingState.Idle)
    val savingState: StateFlow<SavingState> = _savingState

    private val _errorState = MutableStateFlow<String?>(null)
    val errorState: StateFlow<String?> = _errorState

    fun getAllHarvestPlans(): Flow<List<HarvestPlanningWithBuyers>> =
        repository.getAllHarvestPlans()

    suspend fun getHarvestPlanById(id: Long): HarvestPlanningWithBuyers? =
        repository.getHarvestPlanById(id)

    fun saveHarvestPlan(
        harvestPlanning: HarvestPlanning,
        buyers: List<HarvestPlanningBuyer>
    ) {
        viewModelScope.launch {
            try {
                _savingState.value = SavingState.Saving
                val isOnline = NetworkUtils.isNetworkAvailable(getApplication())

                repository.saveHarvestPlan(harvestPlanning, buyers, isOnline)
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

    fun updateHarvestPlan(
        harvestPlanning: HarvestPlanning,
        buyers: List<HarvestPlanningBuyer>
    ) {
        viewModelScope.launch {
            try {
                _savingState.value = SavingState.Saving
                repository.updateHarvestPlan(harvestPlanning, buyers)
                _savingState.value = SavingState.Success
            } catch (e: Exception) {
                _errorState.value = e.message ?: "Unknown error occurred"
                _savingState.value = SavingState.Error
            }
        }
    }

    fun deleteHarvestPlan(harvestPlanning: HarvestPlanning) {
        viewModelScope.launch {
            try {
                repository.deleteHarvestPlan(harvestPlanning.id)
            } catch (e: Exception) {
                _errorState.value = e.message ?: "Error deleting harvest plan"
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

    fun updateFormField(field: String, value: String) {
        when (field) {
            "startTime" -> {
                // Update the start time field
                // Add your logic here
            }
            "endTime" -> {
                // Update the end time field
                // Add your logic here
            }
            "harvestedUnits" -> {
                // Update the harvested units field
                // Add your logic here
            }
            "harvestedQuality" -> {
                // Update the harvested quality field
                // Add your logic here
            }
            else -> {
                // Handle unknown fields
                throw IllegalArgumentException("Unknown field: $field")
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

    companion object {
        fun provideFactory(
            application: Application,
            repository: HarvestManagementRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(HarvestManagementViewModel::class.java)) {
                    return HarvestManagementViewModel(application, repository) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}