package com.fin.mobile

import com.fin.mobile.data.remote.NetworkResult
import com.fin.mobile.data.remote.dto.AuthResponseDto
import com.fin.mobile.data.remote.dto.UserDto
import com.fin.mobile.data.repository.AuthRepository
import com.fin.mobile.ui.auth.AuthViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
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
class AuthValidationTest {

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
    fun testLoginEmailValidation() {
        viewModel.onLoginEmailChanged("not-an-email")
        assertEquals("Invalid email format", viewModel.loginState.value.emailError)

        viewModel.onLoginEmailChanged("citizen@fin.gov.in")
        assertNull(viewModel.loginState.value.emailError)
    }

    @Test
    fun testLoginPasswordValidation() {
        viewModel.onLoginPasswordChanged("123")
        assertEquals("Password must be at least 6 characters", viewModel.loginState.value.passwordError)

        viewModel.onLoginPasswordChanged("securePassword123")
        assertNull(viewModel.loginState.value.passwordError)
    }

    @Test
    fun testLoginFormValidity() {
        viewModel.onLoginEmailChanged("citizen@fin.gov.in")
        viewModel.onLoginPasswordChanged("password123")
        assertTrue(viewModel.loginState.value.isValid)

        viewModel.onLoginEmailChanged("bad-email")
        assertFalse(viewModel.loginState.value.isValid)
    }

    @Test
    fun testSignupFormValidation() {
        viewModel.onSignupFullNameChanged("R")
        assertEquals("Name is too short", viewModel.signupState.value.fullNameError)

        viewModel.onSignupFullNameChanged("Rajesh Sharma")
        assertNull(viewModel.signupState.value.fullNameError)

        viewModel.onSignupEmailChanged("rajesh@fin.gov.in")
        assertNull(viewModel.signupState.value.emailError)

        viewModel.onSignupPasswordChanged("pass123")
        assertNull(viewModel.signupState.value.passwordError)

        viewModel.onSignupConfirmPasswordChanged("mismatchedPass")
        assertEquals("Passwords do not match", viewModel.signupState.value.confirmPasswordError)

        viewModel.onSignupConfirmPasswordChanged("pass123")
        assertNull(viewModel.signupState.value.confirmPasswordError)

        assertTrue(viewModel.signupState.value.isValid)
    }

    @Test
    fun testSignupPhoneValidation() {
        viewModel.onSignupPhoneChanged("abc")
        assertEquals("Invalid phone number format", viewModel.signupState.value.phoneError)

        viewModel.onSignupPhoneChanged("+919876543210")
        assertNull(viewModel.signupState.value.phoneError)
    }
}

class FakeAuthRepository : AuthRepository {
    var hasSession = false
    var currentStoredRole: String? = null
    var currentStoredUserId: String? = null

    var loginResult: NetworkResult<AuthResponseDto> = NetworkResult.Error(401, "Default mock error")
    var registerResult: NetworkResult<AuthResponseDto> = NetworkResult.Error(400, "Default mock error")
    var verifyResult: NetworkResult<UserDto> = NetworkResult.Error(401, "No session")

    override suspend fun login(email: String, password: String, mode: String): NetworkResult<AuthResponseDto> = loginResult

    override suspend fun register(email: String, password: String, fullName: String, phone: String?): NetworkResult<AuthResponseDto> = registerResult

    override suspend fun verifyOrRestoreSession(): NetworkResult<UserDto> = verifyResult

    override suspend fun logout(): NetworkResult<Unit> {
        hasSession = false
        currentStoredRole = null
        currentStoredUserId = null
        return NetworkResult.Success(Unit)
    }

    override fun hasActiveSession(): Boolean = hasSession
    override fun getStoredUserRole(): String? = currentStoredRole
    override fun getStoredUserId(): String? = currentStoredUserId
}
