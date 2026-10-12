package com.fin.mobile.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fin.mobile.data.remote.NetworkResult
import com.fin.mobile.data.remote.dto.DashboardDataDto
import com.fin.mobile.data.repository.DashboardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class CitizenDashboardUiState {
    data object Loading : CitizenDashboardUiState()
    data class Success(val data: DashboardDataDto, val isRefreshing: Boolean = false) : CitizenDashboardUiState()
    data class Error(val message: String) : CitizenDashboardUiState()
}

class CitizenDashboardViewModel(
    private val dashboardRepository: DashboardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<CitizenDashboardUiState>(CitizenDashboardUiState.Loading)
    val uiState: StateFlow<CitizenDashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.value = CitizenDashboardUiState.Loading
            fetchDashboardInternal()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val current = _uiState.value
            if (current is CitizenDashboardUiState.Success) {
                _uiState.value = current.copy(isRefreshing = true)
            }
            fetchDashboardInternal()
        }
    }

    private suspend fun fetchDashboardInternal() {
        when (val result = dashboardRepository.getDashboardData()) {
            is NetworkResult.Success -> {
                _uiState.value = CitizenDashboardUiState.Success(result.data, isRefreshing = false)
            }
            is NetworkResult.Error -> {
                _uiState.value = CitizenDashboardUiState.Error(
                    result.message.ifBlank { "Failed to load dashboard (${result.code})" }
                )
            }
            is NetworkResult.Exception -> {
                _uiState.value = CitizenDashboardUiState.Error(
                    result.throwable.localizedMessage ?: "Unable to connect to dashboard service"
                )
            }
        }
    }

    companion object {
        fun provideFactory(dashboardRepository: DashboardRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return CitizenDashboardViewModel(dashboardRepository) as T
                }
            }
    }
}
