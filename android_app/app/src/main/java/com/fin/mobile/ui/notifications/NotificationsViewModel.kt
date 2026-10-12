package com.fin.mobile.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fin.mobile.data.remote.NetworkResult
import com.fin.mobile.data.remote.dto.NotificationDto
import com.fin.mobile.data.repository.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class NotificationFilter {
    ALL,
    UNREAD
}

data class NotificationsUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isPerformingAction: Boolean = false,
    val notifications: List<NotificationDto> = emptyList(),
    val unreadCount: Int = 0,
    val filter: NotificationFilter = NotificationFilter.ALL,
    val errorMessage: String? = null,
    val successMessage: String? = null
) {
    val displayedNotifications: List<NotificationDto>
        get() = when (filter) {
            NotificationFilter.ALL -> notifications
            NotificationFilter.UNREAD -> notifications.filter { !it.isRead }
        }
}

class NotificationsViewModel(
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState(isLoading = true))
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    init {
        loadNotifications()
    }

    fun loadNotifications(isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isRefresh) {
                _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
            } else {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            }

            when (val result = notificationRepository.getNotifications()) {
                is NetworkResult.Success -> {
                    val notifData = result.data
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            notifications = notifData.data,
                            unreadCount = notifData.unreadCount,
                            errorMessage = null
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = result.message
                        )
                    }
                }
                is NetworkResult.Exception -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = "Network error: Unable to load notifications"
                        )
                    }
                }
            }
        }
    }

    fun setFilter(filter: NotificationFilter) {
        _uiState.update { it.copy(filter = filter) }
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            // Optimistically update read state in UI
            _uiState.update { current ->
                val updatedList = current.notifications.map { notif ->
                    if (notif.id == notificationId) notif.copy(read = true, unread = false) else notif
                }
                val newUnread = updatedList.count { !it.isRead }
                current.copy(notifications = updatedList, unreadCount = newUnread)
            }

            when (val result = notificationRepository.markNotificationRead(notificationId)) {
                is NetworkResult.Success -> {
                    // Confirmed by server
                }
                is NetworkResult.Error -> {
                    // Revert or show notification
                    _uiState.update { it.copy(errorMessage = result.message) }
                    loadNotifications(isRefresh = true)
                }
                is NetworkResult.Exception -> {
                    _uiState.update { it.copy(errorMessage = "Network error: Could not update notification") }
                    loadNotifications(isRefresh = true)
                }
            }
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            if (_uiState.value.unreadCount == 0) return@launch
            _uiState.update { it.copy(isPerformingAction = true, errorMessage = null) }

            when (val result = notificationRepository.markAllNotificationsRead()) {
                is NetworkResult.Success -> {
                    _uiState.update { current ->
                        val allReadList = current.notifications.map { it.copy(read = true, unread = false) }
                        current.copy(
                            isPerformingAction = false,
                            notifications = allReadList,
                            unreadCount = 0,
                            successMessage = result.data
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isPerformingAction = false,
                            errorMessage = result.message
                        )
                    }
                }
                is NetworkResult.Exception -> {
                    _uiState.update {
                        it.copy(
                            isPerformingAction = false,
                            errorMessage = "Network error: Could not mark all notifications as read"
                        )
                    }
                }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun dismissSuccess() {
        _uiState.update { it.copy(successMessage = null) }
    }

    companion object {
        fun provideFactory(notificationRepository: NotificationRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return NotificationsViewModel(notificationRepository) as T
                }
            }
    }
}
