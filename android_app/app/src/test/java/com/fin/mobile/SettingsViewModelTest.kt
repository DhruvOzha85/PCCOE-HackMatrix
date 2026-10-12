package com.fin.mobile

import com.fin.mobile.data.remote.NetworkResult
import com.fin.mobile.data.remote.dto.AuthResponseDto
import com.fin.mobile.data.remote.dto.UserDto
import com.fin.mobile.data.remote.dto.UserProfileDto
import com.fin.mobile.data.repository.AuthRepository
import com.fin.mobile.ui.settings.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeAuthRepository: MockAuthRepository
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeAuthRepository = MockAuthRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialLoadProfileAndUser() = runTest {
        fakeAuthRepository.mockUser = UserDto(id = "u1", email = "test@fin.gov.in", role = "citizen")
        fakeAuthRepository.mockProfile = UserProfileDto(
            id = "u1",
            fullName = "Rajesh Sharma",
            phone = "9876543210",
            state = "Maharashtra",
            district = "Pune",
            occupation = "farmer",
            annualIncome = 180000.0
        )

        viewModel = SettingsViewModel(fakeAuthRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoadingProfile)
        assertNotNull(state.profile)
        assertEquals("Rajesh Sharma", state.profile?.fullName)
        assertEquals("Pune", state.profile?.district)
        assertEquals("test@fin.gov.in", state.user?.email)
    }

    @Test
    fun testUpdateProfileSuccess() = runTest {
        viewModel = SettingsViewModel(fakeAuthRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        fakeAuthRepository.updateProfileResult = NetworkResult.Success(
            UserProfileDto(
                id = "u1",
                fullName = "Rajesh Sharma",
                phone = "9876543210",
                state = "Maharashtra",
                district = "Mumbai",
                occupation = "student",
                annualIncome = 0.0
            )
        )

        viewModel.updateProfile("9876543210", "Maharashtra", "Mumbai", "student", 0.0)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isUpdatingProfile)
        assertFalse(state.isEditProfileDialogOpen)
        assertEquals("Mumbai", state.profile?.district)
        assertEquals("Profile updated successfully", state.successMessage)
    }

    @Test
    fun testChangePasswordValidationRejectsEmptyCurrentPassword() = runTest {
        viewModel = SettingsViewModel(fakeAuthRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.changePassword("", "newPassword123", "newPassword123")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Current password is required", state.passwordError)
        assertFalse(state.isChangingPassword)
    }

    @Test
    fun testChangePasswordValidationRejectsShortPassword() = runTest {
        viewModel = SettingsViewModel(fakeAuthRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.changePassword("current123", "short", "short")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("New password must be at least 6 characters", state.passwordError)
        assertFalse(state.isChangingPassword)
    }

    @Test
    fun testChangePasswordValidationRejectsMismatch() = runTest {
        viewModel = SettingsViewModel(fakeAuthRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.changePassword("current123", "newPass123", "differentPass123")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("New password and confirmation do not match", state.passwordError)
        assertFalse(state.isChangingPassword)
    }

    @Test
    fun testChangePasswordValidationRejectsSameAsCurrent() = runTest {
        viewModel = SettingsViewModel(fakeAuthRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.changePassword("samePassword123", "samePassword123", "samePassword123")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("New password must be different from current password", state.passwordError)
        assertFalse(state.isChangingPassword)
    }

    @Test
    fun testChangePasswordServerSuccess() = runTest {
        viewModel = SettingsViewModel(fakeAuthRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        fakeAuthRepository.changePasswordResult = NetworkResult.Success("Password changed successfully")

        viewModel.changePassword("oldPass123", "newPass123", "newPass123")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isChangingPassword)
        assertFalse(state.isPasswordDialogOpen)
        assertEquals("Password changed successfully", state.passwordSuccess)
    }

    @Test
    fun testSetLanguagePersistsPreference() = runTest {
        viewModel = SettingsViewModel(fakeAuthRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.setLanguage("hi")

        assertEquals("hi", fakeAuthRepository.getLanguage())
        assertEquals("hi", viewModel.uiState.value.language)
        assertTrue(viewModel.uiState.value.successMessage?.contains("हिंदी") == true)
    }

    @Test
    fun testLogoutCallsRepositoryAndClearsState() = runTest {
        viewModel = SettingsViewModel(fakeAuthRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        var loggedOutCallbackTriggered = false
        viewModel.logout { loggedOutCallbackTriggered = true }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(fakeAuthRepository.logoutCalled)
        assertTrue(loggedOutCallbackTriggered)
    }
}

class MockAuthRepository : AuthRepository {
    var mockUser: UserDto = UserDto(id = "mock-id", email = "test@fin.gov.in", role = "citizen")
    var mockProfile: UserProfileDto = UserProfileDto(id = "mock-id", fullName = "Mock Citizen")
    var updateProfileResult: NetworkResult<UserProfileDto> = NetworkResult.Success(mockProfile)
    var changePasswordResult: NetworkResult<String> = NetworkResult.Success("Password updated successfully")
    var logoutCalled = false
    private var lang = "en"

    override suspend fun login(email: String, password: String, mode: String): NetworkResult<AuthResponseDto> =
        NetworkResult.Success(AuthResponseDto(success = true))

    override suspend fun register(email: String, password: String, fullName: String, phone: String?): NetworkResult<AuthResponseDto> =
        NetworkResult.Success(AuthResponseDto(success = true))

    override suspend fun verifyOrRestoreSession(): NetworkResult<UserDto> = NetworkResult.Success(mockUser)

    override suspend fun logout(): NetworkResult<Unit> {
        logoutCalled = true
        return NetworkResult.Success(Unit)
    }

    override suspend fun getProfile(): NetworkResult<UserProfileDto> = NetworkResult.Success(mockProfile)

    override suspend fun updateProfile(updates: Map<String, Any?>): NetworkResult<UserProfileDto> = updateProfileResult

    override suspend fun changePassword(currentPassword: String, newPassword: String): NetworkResult<String> = changePasswordResult

    override fun hasActiveSession(): Boolean = true
    override fun getStoredUserRole(): String = "citizen"
    override fun getStoredUserId(): String = "mock-id"
    override fun getLanguage(): String = lang
    override fun setLanguage(language: String) {
        lang = language
    }
}
