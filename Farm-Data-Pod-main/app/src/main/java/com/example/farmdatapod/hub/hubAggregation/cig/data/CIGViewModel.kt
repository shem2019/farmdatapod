package com.example.farmdatapod.hub.hubAggregation.cig.data

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.farmdatapod.FarmDataPodApplication
import com.example.farmdatapod.models.MemberRequest
import com.example.farmdatapod.sync.SyncManager
import com.google.gson.Gson
import kotlinx.coroutines.launch

// This sealed class represents the state of the UI, making it easy for the Fragment to react.
sealed class RegistrationState {
    object Idle : RegistrationState()
    object Loading : RegistrationState()
    data class Success(val message: String) : RegistrationState()
    data class Error(val errorMessage: String) : RegistrationState()
}

class CIGViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "CIGViewModel"

    // Get singleton instances from the Application class
    private val cigRepository: CIGRepository = CIGRepository(application.applicationContext)
    private val syncManager: SyncManager = (application as FarmDataPodApplication).syncManager

    private val gson = Gson()

    private val _registrationState = MutableLiveData<RegistrationState>(RegistrationState.Idle)
    val registrationState: LiveData<RegistrationState> = _registrationState

    /**
     * This is the primary function called by the CIGFragment to save a new CIG.
     * It follows a strict offline-first approach:
     * 1. Saves the data to the local Room database with `syncStatus = false`.
     * 2. Triggers the SyncManager to attempt an immediate background sync if online.
     * 3. Immediately gives the user feedback that their data is safe.
     */
    fun submitCIGData(cigData: CIG, membersData: List<MemberRequest>) {
        _registrationState.value = RegistrationState.Loading
        Log.d(TAG, "submitCIGData called for CIG: ${cigData.cigName}")

        viewModelScope.launch {
            // Convert the list of members to a JSON string for local storage
            val membersJson = gson.toJson(membersData)
            val localCigToSave = cigData.copy(
                membersJson = membersJson,
                syncStatus = false // Always save as 'unsynced' to begin with
            )

            // Step 1: Save the CIG to the local database.
            val localSaveResult = cigRepository.saveCIGLocally(localCigToSave)

            localSaveResult.fold(
                onSuccess = { localId ->
                    Log.i(TAG, "Successfully saved CIG to local DB with ID: $localId. Now triggering sync.")

                    // Step 2: Trigger the immediate sync worker.
                    // This will run as soon as network is available.
                    // The actual success/failure of the network call is handled by the worker and repository.
                    syncManager.triggerImmediateSync()

                    // Step 3: Give immediate success feedback to the user.
                    val successMessage = "CIG data has been saved. It will sync with the server automatically."
                    _registrationState.postValue(RegistrationState.Success(successMessage))
                },
                onFailure = { error ->
                    val errorMessage = "Critical Error: Could not save CIG data locally. ${error.message}"
                    _registrationState.postValue(RegistrationState.Error(errorMessage))
                    Log.e(TAG, errorMessage, error)
                }
            )
        }
    }
}