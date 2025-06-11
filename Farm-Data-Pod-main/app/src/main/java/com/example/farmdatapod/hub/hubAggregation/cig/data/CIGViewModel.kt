package com.example.farmdatapod.hub.hubAggregation.cig

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.farmdatapod.FarmDataPodApplication
import com.example.farmdatapod.hub.hubAggregation.cig.data.CIG
import com.example.farmdatapod.hub.hubAggregation.cig.data.CIGRepository
import com.example.farmdatapod.models.MemberRequest
import com.example.farmdatapod.sync.SyncManager
import com.google.gson.Gson
import kotlinx.coroutines.launch

sealed class RegistrationState {
    object Idle : RegistrationState()
    object Loading : RegistrationState()
    data class Success(val message: String) : RegistrationState()
    data class Error(val errorMessage: String) : RegistrationState()
}

class CIGViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "CIGViewModel"
    private val cigRepository = CIGRepository(application.applicationContext)
    private val syncManager = (application as FarmDataPodApplication).syncManager
    private val gson = Gson()

    private val _registrationState = MutableLiveData<RegistrationState>(RegistrationState.Idle)
    val registrationState: LiveData<RegistrationState> = _registrationState

    fun submitCIGData(cigData: CIG, membersData: List<MemberRequest>) {
        _registrationState.value = RegistrationState.Loading
        Log.d(TAG, "submitCIGData called for CIG: ${cigData.cigName}")

        viewModelScope.launch {
            val membersJson = gson.toJson(membersData)
            val localCigToSave = cigData.copy(
                membersJson = membersJson,
                syncStatus = false
            )

            val localSaveResult = cigRepository.saveCIGLocally(localCigToSave)

            localSaveResult.fold(
                onSuccess = {
                    val successMessage = "CIG data saved. Attempting to sync in the background."
                    _registrationState.postValue(RegistrationState.Success(successMessage))
                    Log.d(TAG, successMessage)
                    syncManager.triggerImmediateSync()
                },
                onFailure = { error ->
                    val errorMessage = "Error: Could not save CIG locally. ${error.message}"
                    _registrationState.postValue(RegistrationState.Error(errorMessage))
                }
            )
        }
    }
}