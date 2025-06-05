package com.example.farmdatapod.logistics.inboundAndOutbound.outboundLogistics.loadedInput.data


sealed class LoadingInputUiState {
    object Initial : LoadingInputUiState()
    object Loading : LoadingInputUiState()
    data class Success(val message: String) : LoadingInputUiState()
    data class Error(val message: String) : LoadingInputUiState()
}