package com.fin.mobile.ui.applications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fin.mobile.data.remote.NetworkResult
import com.fin.mobile.data.remote.dto.ApplicationDto
import com.fin.mobile.data.repository.ApplicationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ApplicationCounts(
    val total: Int = 0,
    val underReview: Int = 0,
    val actionRequired: Int = 0,
    val approved: Int = 0,
    val rejected: Int = 0
)

data class MyApplicationsUiState(
    val isLoading: Boolean = true,
    val applications: List<ApplicationDto> = emptyList(),
    val filteredApplications: List<ApplicationDto> = emptyList(),
    val searchQuery: String = "",
    val selectedStatus: String = "all", // all, under_review, action_required, approved, rejected
    val counts: ApplicationCounts = ApplicationCounts(),
    val errorMessage: String? = null
)

class MyApplicationsViewModel(
    private val applicationRepository: ApplicationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyApplicationsUiState())
    val uiState: StateFlow<MyApplicationsUiState> = _uiState.asStateFlow()

    init {
        loadApplications()
    }

    fun loadApplications() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = applicationRepository.listApplications()) {
                is NetworkResult.Success -> {
                    val apps = result.data
                    val counts = ApplicationCounts(
                        total = apps.size,
                        underReview = apps.count { it.normalizedStatus in setOf("under_review", "submitted", "pending") },
                        actionRequired = apps.count { it.normalizedStatus == "action_required" },
                        approved = apps.count { it.normalizedStatus in setOf("approved", "sanctioned") },
                        rejected = apps.count { it.normalizedStatus == "rejected" }
                    )
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            applications = apps,
                            counts = counts,
                            errorMessage = null
                        )
                    }
                    applyFilters()
                }
                is NetworkResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.message.ifBlank { "Failed to load applications (${result.code})" }
                        )
                    }
                }
                is NetworkResult.Exception -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.throwable.localizedMessage ?: "Network connection error"
                        )
                    }
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applyFilters()
    }

    fun onStatusFilterChanged(status: String) {
        _uiState.update { it.copy(selectedStatus = status) }
        applyFilters()
    }

    private fun applyFilters() {
        val current = _uiState.value
        val query = current.searchQuery.trim().lowercase()
        val status = current.selectedStatus.lowercase()

        val filtered = current.applications.filter { app ->
            val matchesQuery = query.isEmpty() ||
                app.displayTitle.lowercase().contains(query) ||
                app.referenceId.lowercase().contains(query) ||
                (app.category?.lowercase()?.contains(query) == true)

            val matchesStatus = when (status) {
                "all" -> true
                "under_review" -> app.normalizedStatus in setOf("under_review", "submitted", "pending")
                "action_required" -> app.normalizedStatus == "action_required"
                "approved" -> app.normalizedStatus in setOf("approved", "sanctioned")
                "rejected" -> app.normalizedStatus == "rejected"
                "draft" -> app.normalizedStatus == "draft"
                else -> true
            }

            matchesQuery && matchesStatus
        }

        _uiState.update { it.copy(filteredApplications = filtered) }
    }

    companion object {
        fun provideFactory(applicationRepository: ApplicationRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return MyApplicationsViewModel(applicationRepository) as T
                }
            }
    }
}
