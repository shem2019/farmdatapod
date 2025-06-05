package com.example.farmdatapod.hub.hubAggregation.buyingCenter.data



import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.farmdatapod.hub.hubRegistration.data.Hub
import com.example.farmdatapod.hub.hubRegistration.data.HubRepository
import com.example.farmdatapod.utils.NetworkUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class BuyingCenterViewModel(application: Application) : AndroidViewModel(application) {

    private val buyingCenterRepository = BuyingCenterRepository(application)
    private val hubRepository = HubRepository(application)
    var yearEstablishedApiFormat: String? = null

    private val _savingState = MutableStateFlow<SavingState>(SavingState.Idle)
    val savingState: StateFlow<SavingState> = _savingState

    private val _errorState = MutableStateFlow<String?>(null)
    val errorState: StateFlow<String?> = _errorState

    // Flow for hubs list
    val hubs: Flow<List<Hub>> = hubRepository.getAllHubs()

    fun saveBuyingCenter(buyingCenter: BuyingCenterEntity) {
        viewModelScope.launch {
            try {
                _savingState.value = SavingState.Saving
                val isOnline = NetworkUtils.isNetworkAvailable(getApplication())

                buyingCenterRepository.saveBuyingCenter(buyingCenter, isOnline)
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

    fun getBuyingCentersByHub(hubId: Int): Flow<List<BuyingCenterEntity>> {
        return buyingCenterRepository.getBuyingCentersByHubId(hubId)
    }

    fun getAllBuyingCenters(): Flow<List<BuyingCenterEntity>> {
        return buyingCenterRepository.getAllBuyingCenters()
    }

    suspend fun getBuyingCenterById(id: Int): BuyingCenterEntity? {
        return buyingCenterRepository.getBuyingCenterById(id)
    }

    fun syncWithServer() {
        viewModelScope.launch {
            try {
                buyingCenterRepository.performFullSync()
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
