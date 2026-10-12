package com.fin.mobile

import com.fin.mobile.data.remote.NetworkResult
import com.fin.mobile.data.remote.dto.AuthResponseDto
import com.fin.mobile.data.remote.dto.UserDto
import com.fin.mobile.ui.auth.AuthState
import com.fin.mobile.ui.auth.AuthViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeAuthRepository
    private lateinit var viewModel: AuthViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeAuthRepository()
        viewModel = AuthViewModel(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testSessionRestorationWhenNoSession() = runTest {
        fakeRepository.hasSession = false
        viewModel.restoreSession()
        advanceUntilIdle()

        assertTrue(viewModel.authState.value is AuthState.Unauthenticated)
    }

    @Test
    fun testSessionRestorationWhenValidSessionExists() = runTest {
        fakeRepository.hasSession = true
        fakeRepository.verifyResult = NetworkResult.Success(
            UserDto(id = "user-123", email = "citizen@fin.gov.in", role = "user")
        )

        viewModel.restoreSession()
        advanceUntilIdle()

        val state = viewModel.authState.value
        assertTrue(state is AuthState.Authenticated)
        val auth = state as AuthState.Authenticated
        assertEquals("user", auth.role)
        assertEquals("citizen@fin.gov.in", auth.user.email)
    }

    @Test
    fun testSubmitLoginSuccessRoutesToCorrectRole() = runTest {
        var navigatedRole: String? = null
        fakeRepository.loginResult = NetworkResult.Success(
            AuthResponseDto(
                success = true,
                accessToken = "jwt_token",
                user = UserDto(id = "rev-1", email = "officer@fin.gov.in", role = "reviewer")
            )
        )

        viewModel.onLoginEmailChanged("officer@fin.gov.in")
        viewModel.onLoginPasswordChanged("officerPassword")
        viewModel.onLoginModeChanged("reviewer")

        viewModel.submitLogin { role ->
            navigatedRole = role
        }
        advanceUntilIdle()

        assertEquals("reviewer", navigatedRole)
        assertTrue(viewModel.authState.value is AuthState.Authenticated)
    }

    @Test
    fun testSubmitLoginFailureDisplaysErrorMessage() = runTest {
        fakeRepository.loginResult = NetworkResult.Error(
            code = 401,
            message = "Invalid credentials"
        )

        viewModel.onLoginEmailChanged("user@fin.gov.in")
        viewModel.onLoginPasswordChanged("wrongpassword")

        viewModel.submitLogin()
        advanceUntilIdle()

        assertEquals("Invalid credentials", viewModel.loginState.value.generalError)
        assertTrue(viewModel.authState.value is AuthState.Error)
    }

    @Test
    fun testSubmitSignupSuccessRoutesToCitizenRole() = runTest {
        var navigatedRole: String? = null
        fakeRepository.registerResult = NetworkResult.Success(
            AuthResponseDto(
                success = true,
                accessToken = "jwt_reg_token",
                user = UserDto(id = "cit-1", email = "newcitizen@fin.gov.in", role = "user")
            )
        )

        viewModel.onSignupFullNameChanged("Anita Patel")
        viewModel.onSignupEmailChanged("newcitizen@fin.gov.in")
        viewModel.onSignupPhoneChanged("9876543210")
        viewModel.onSignupPasswordChanged("mypassword")
        viewModel.onSignupConfirmPasswordChanged("mypassword")

        viewModel.submitSignup { role ->
            navigatedRole = role
        }
        advanceUntilIdle()

        assertEquals("user", navigatedRole)
        assertTrue(viewModel.authState.value is AuthState.Authenticated)
    }

    @Test
    fun testLogoutResetsAuthStateToUnauthenticated() = runTest {
        var logoutCompleted = false
        viewModel.logout {
            logoutCompleted = true
        }
        advanceUntilIdle()

        assertTrue(logoutCompleted)
        assertTrue(viewModel.authState.value is AuthState.Unauthenticated)
    }
}
