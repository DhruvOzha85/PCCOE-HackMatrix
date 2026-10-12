package com.fin.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fin.mobile.data.local.EncryptedSessionStorage
import com.fin.mobile.data.remote.NetworkModule
import com.fin.mobile.data.repository.AuthRepositoryImpl
import com.fin.mobile.data.repository.DashboardRepositoryImpl
import com.fin.mobile.data.repository.DocumentRepositoryImpl
import com.fin.mobile.data.repository.NotificationRepositoryImpl
import com.fin.mobile.data.repository.SchemeRepositoryImpl
import com.fin.mobile.ui.auth.AuthViewModel
import com.fin.mobile.ui.navigation.AppNavigation
import com.fin.mobile.ui.theme.FINTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val sessionStorage = EncryptedSessionStorage(applicationContext)
        val moshi = NetworkModule.createMoshi()
        val apiService = NetworkModule.createApiService(sessionStorage)
        val authRepository = AuthRepositoryImpl(
            apiService = apiService,
            sessionStorage = sessionStorage,
            moshi = moshi
        )
        val dashboardRepository = DashboardRepositoryImpl(
            apiService = apiService,
            moshi = moshi
        )
        val schemeRepository = SchemeRepositoryImpl(
            apiService = apiService,
            moshi = moshi
        )
        val documentRepository = DocumentRepositoryImpl(
            apiService = apiService,
            moshi = moshi
        )
        val applicationRepository = com.fin.mobile.data.repository.ApplicationRepositoryImpl(
            apiService = apiService,
            moshi = moshi
        )
        val chatRepository = com.fin.mobile.data.repository.ChatRepositoryImpl(
            apiService = apiService,
            moshi = moshi
        )
        val reviewerRepository = com.fin.mobile.data.repository.ReviewerRepositoryImpl(
            apiService = apiService,
            moshi = moshi
        )
        val notificationRepository = NotificationRepositoryImpl(
            apiService = apiService,
            moshi = moshi
        )
        val viewModelFactory = AuthViewModel.provideFactory(authRepository)

        setContent {
            FINTheme {
                val authViewModel: AuthViewModel = viewModel(factory = viewModelFactory)

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppNavigation(
                        authViewModel = authViewModel,
                        authRepository = authRepository,
                        dashboardRepository = dashboardRepository,
                        schemeRepository = schemeRepository,
                        documentRepository = documentRepository,
                        applicationRepository = applicationRepository,
                        chatRepository = chatRepository,
                        reviewerRepository = reviewerRepository,
                        notificationRepository = notificationRepository,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }
        }
    }
}