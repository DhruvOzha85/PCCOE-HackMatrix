package com.fin.mobile.ui.reviewer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fin.mobile.data.remote.NetworkResult
import com.fin.mobile.data.remote.dto.ApplicationDto
import com.fin.mobile.data.remote.dto.ReviewerDashboardStatsDto
import com.fin.mobile.data.repository.ReviewerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReviewerDashboardUiState(
    val stats: ReviewerDashboardStatsDto = ReviewerDashboardStatsDto(),
    val applications: List<ApplicationDto> = emptyList(),
    val selectedStatus: String = "all", // "all", "pending", "action_required", "approved", "rejected"
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null
) {
    val filteredApplications: List<ApplicationDto>
        get() {
            if (searchQuery.isBlank()) return applications
            val query = searchQuery.trim().lowercase()
            return applications.filter { app ->
                (app.applicantName?.lowercase()?.contains(query) == true) ||
                ((app.applicationNumber ?: app.referenceId).lowercase().contains(query)) ||
                (app.schemeName?.lowercase()?.contains(query) == true)
            }
        }
}

class ReviewerDashboardViewModel(
    private val reviewerRepository: ReviewerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReviewerDashboardUiState())
    val uiState: StateFlow<ReviewerDashboardUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData(isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isRefresh) {
                _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
            } else {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            }

            // Load dashboard metrics
            val statsResult = reviewerRepository.getDashboardStats()
            // Load queue for currently selected status
            val queueResult = reviewerRepository.getReviewQueue(
                status = _uiState.value.selectedStatus
            )

            var error: String? = null
            var updatedStats = _uiState.value.stats
            var updatedApps = _uiState.value.applications

            when (statsResult) {
                is NetworkResult.Success -> {
                    updatedStats = statsResult.data
                }
                is NetworkResult.Error -> {
                    error = statsResult.message ?: "Failed to retrieve reviewer metrics"
                }
                is NetworkResult.Exception -> {
                    error = statsResult.throwable.message ?: "Network error loading metrics"
                }
            }

            when (queueResult) {
                is NetworkResult.Success -> {
                    updatedApps = queueResult.data
                }
                is NetworkResult.Error -> {
                    if (error == null) {
                        error = queueResult.message ?: "Failed to load review queue"
                    }
                }
                is NetworkResult.Exception -> {
                    if (error == null) {
                        error = queueResult.throwable.message ?: "Network error loading queue"
                    }
                }
            }

            _uiState.update {
                it.copy(
                    stats = updatedStats,
                    applications = updatedApps,
                    isLoading = false,
                    isRefreshing = false,
                    errorMessage = error
                )
            }
        }
    }

    fun setStatusFilter(status: String) {
        if (_uiState.value.selectedStatus == status) return
        _uiState.update { it.copy(selectedStatus = status) }
        loadQueueOnly(status)
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    private fun loadQueueOnly(status: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val queueResult = reviewerRepository.getReviewQueue(status = status)) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            applications = queueResult.data,
                            isLoading = false
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = queueResult.message ?: "Failed to filter applications"
                        )
                    }
                }
                is NetworkResult.Exception -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = queueResult.throwable.message ?: "Network error"
                        )
                    }
                }
            }
        }
    }

    companion object {
        fun provideFactory(reviewerRepository: ReviewerRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ReviewerDashboardViewModel(reviewerRepository) as T
                }
            }
    }
}
