package com.example.farmdatapod.hub.hubAggregation.cig

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.farmdatapod.hub.hubAggregation.cig.data.CIG
import com.example.farmdatapod.hub.hubAggregation.cig.data.CIGRepository
import com.example.farmdatapod.models.MemberRequest
import com.google.gson.Gson
import kotlinx.coroutines.launch

// Sealed class to represent UI state feedback
sealed class RegistrationState {
    object Idle : RegistrationState()
    object Loading : RegistrationState()
    data class Success(val message: String) : RegistrationState()
    data class Error(val errorMessage: String) : RegistrationState()
}

class CIGViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "CIGViewModel"
    private val cigRepository: CIGRepository = CIGRepository(application.applicationContext)
    private val gson = Gson()

    private val _registrationState = MutableLiveData<RegistrationState>(RegistrationState.Idle)
    val registrationState: LiveData<RegistrationState> = _registrationState

    /**
     * The primary function called by the Fragment to save CIG data.
     * It handles saving the data locally for an offline-first experience.
     * The SyncManager will handle pushing the data to the server later.
     */
    fun submitCIGData(cigData: CIG, membersData: List<MemberRequest>) {
        _registrationState.value = RegistrationState.Loading

        viewModelScope.launch {
            // Convert the list of members to a JSON string for local storage
            val membersJson = gson.toJson(membersData)
            val localCig = cigData.copy(
                membersJson = membersJson,
                syncStatus = false // Always save as unsynced
            )

            // Save locally. This is the main action for offline-first.
            val localSaveResult = cigRepository.saveCIGLocally(localCig)
            localSaveResult.fold(
                onSuccess = {
                    // Inform the user of success. The app will sync later.
                    val message = "CIG data saved locally. It will be synced when you are online."
                    _registrationState.postValue(RegistrationState.Success(message))
                    Log.d(TAG, message)
                },
                onFailure = { error ->
                    val errorMessage = "Error: Could not save CIG data locally. ${error.message}"
                    _registrationState.postValue(RegistrationState.Error(errorMessage))
                    Log.e(TAG, errorMessage)
                }
            )
        }
    }
}