package com.example.farmdatapod.logistics.createRoute.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.farmdatapod.utils.NetworkUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class RouteViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = RouteRepository(application)

    // UI State
    private val _uiState = MutableStateFlow<RouteUiState>(RouteUiState.Initial)
    val uiState: StateFlow<RouteUiState> = _uiState

    // Route List
    private val _routes = MutableLiveData<List<RouteEntity>>()
    val routes: LiveData<List<RouteEntity>> = _routes

    // Loading State
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // Error State
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    init {
        loadRoutes()
    }

    fun createRoute(route: RouteEntity) {
        viewModelScope.launch {
            try {
                _uiState.value = RouteUiState.Loading

                // Save route locally first
                repository.saveRoute(route, false)
                    .onSuccess {
                        _uiState.value = RouteUiState.Success("Route created successfully")

                        // If online, sync the route
                        if (NetworkUtils.isNetworkAvailable(getApplication())) {
                            syncRoutes()
                        }
                    }
                    .onFailure { exception ->
                        _uiState.value = RouteUiState.Error(exception.message ?: "Error creating route")
                    }
            } catch (e: Exception) {
                _uiState.value = RouteUiState.Error(e.message ?: "Error creating route")
            }
        }
    }

    fun loadRoutes() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.getAllRoutes()
                    .catch { e ->
                        _error.value = e.message
                    }
                    .collect { routeList ->
                        _routes.value = routeList
                        _isLoading.value = false
                    }
            } catch (e: Exception) {
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }

    fun syncRoutes() {
        viewModelScope.launch {
            try {
                _uiState.value = RouteUiState.Loading
                repository.performFullSync()
                    .onSuccess {
                        _uiState.value = RouteUiState.Success("Routes synced successfully")
                    }
                    .onFailure { exception ->
                        _uiState.value = RouteUiState.Error(exception.message ?: "Error syncing routes")
                    }
            } catch (e: Exception) {
                _uiState.value = RouteUiState.Error(e.message ?: "Error syncing routes")
            }
        }
    }

    fun deleteRoute(route: RouteEntity) {
        viewModelScope.launch {
            try {
                repository.deleteRoute(route)
                _uiState.value = RouteUiState.Success("Route deleted successfully")
            } catch (e: Exception) {
                _uiState.value = RouteUiState.Error(e.message ?: "Error deleting route")
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}

sealed class RouteUiState {
    object Initial : RouteUiState()
    object Loading : RouteUiState()
    data class Success(val message: String) : RouteUiState()
    data class Error(val message: String) : RouteUiState()
}