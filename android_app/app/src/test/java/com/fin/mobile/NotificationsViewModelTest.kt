package com.fin.mobile

import com.fin.mobile.data.remote.NetworkResult
import com.fin.mobile.data.remote.dto.NotificationDto
import com.fin.mobile.data.remote.dto.NotificationListResponseDto
import com.fin.mobile.data.repository.NotificationRepository
import com.fin.mobile.ui.notifications.NotificationFilter
import com.fin.mobile.ui.notifications.NotificationsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeNotificationRepository
    private lateinit var viewModel: NotificationsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeNotificationRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialLoadSuccess() = runTest {
        val sampleList = listOf(
            NotificationDto(id = "1", title = "Update 1", message = "Msg 1", read = false),
            NotificationDto(id = "2", title = "Update 2", message = "Msg 2", read = true)
        )
        fakeRepository.getNotificationsResult = NetworkResult.Success(
            NotificationListResponseDto(success = true, data = sampleList, unreadCount = 1)
        )

        viewModel = NotificationsViewModel(fakeRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(2, state.notifications.size)
        assertEquals(1, state.unreadCount)
        assertNull(state.errorMessage)
    }

    @Test
    fun testFilteringUnreadNotifications() = runTest {
        val sampleList = listOf(
            NotificationDto(id = "1", title = "Update 1", message = "Msg 1", read = false),
            NotificationDto(id = "2", title = "Update 2", message = "Msg 2", read = true)
        )
        fakeRepository.getNotificationsResult = NetworkResult.Success(
            NotificationListResponseDto(success = true, data = sampleList, unreadCount = 1)
        )

        viewModel = NotificationsViewModel(fakeRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.setFilter(NotificationFilter.UNREAD)

        val state = viewModel.uiState.value
        assertEquals(NotificationFilter.UNREAD, state.filter)
        assertEquals(1, state.displayedNotifications.size)
        assertEquals("1", state.displayedNotifications[0].id)
    }

    @Test
    fun testMarkSingleNotificationAsReadOptimistically() = runTest {
        val sampleList = listOf(
            NotificationDto(id = "1", title = "Update 1", message = "Msg 1", read = false)
        )
        fakeRepository.getNotificationsResult = NetworkResult.Success(
            NotificationListResponseDto(success = true, data = sampleList, unreadCount = 1)
        )

        viewModel = NotificationsViewModel(fakeRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.markAsRead("1")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.notifications[0].isRead)
        assertEquals(0, state.unreadCount)
    }

    @Test
    fun testMarkAllAsRead() = runTest {
        val sampleList = listOf(
            NotificationDto(id = "1", title = "Update 1", message = "Msg 1", read = false),
            NotificationDto(id = "2", title = "Update 2", message = "Msg 2", read = false)
        )
        fakeRepository.getNotificationsResult = NetworkResult.Success(
            NotificationListResponseDto(success = true, data = sampleList, unreadCount = 2)
        )

        viewModel = NotificationsViewModel(fakeRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.markAllAsRead()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isPerformingAction)
        assertEquals(0, state.unreadCount)
        assertTrue(state.notifications.all { it.isRead })
    }

    @Test
    fun testInitialLoadError() = runTest {
        fakeRepository.getNotificationsResult = NetworkResult.Error(500, "Server database error")

        viewModel = NotificationsViewModel(fakeRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Server database error", state.errorMessage)
    }
}

class FakeNotificationRepository : NotificationRepository {
    var getNotificationsResult: NetworkResult<NotificationListResponseDto> =
        NetworkResult.Success(NotificationListResponseDto())
    var markReadResult: NetworkResult<String> = NetworkResult.Success("Marked as read")
    var markAllReadResult: NetworkResult<String> = NetworkResult.Success("Marked all as read")

    override suspend fun getNotifications(): NetworkResult<NotificationListResponseDto> = getNotificationsResult
    override suspend fun markNotificationRead(id: String): NetworkResult<String> = markReadResult
    override suspend fun markAllNotificationsRead(): NetworkResult<String> = markAllReadResult
}
