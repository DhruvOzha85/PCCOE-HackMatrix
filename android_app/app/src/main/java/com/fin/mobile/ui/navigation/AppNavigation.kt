package com.fin.mobile.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fin.mobile.data.repository.ApplicationRepository
import com.fin.mobile.data.repository.AuthRepository
import com.fin.mobile.data.repository.ChatRepository
import com.fin.mobile.data.repository.DashboardRepository
import com.fin.mobile.data.repository.DocumentRepository
import com.fin.mobile.data.repository.NotificationRepository
import com.fin.mobile.data.repository.ReviewerRepository
import com.fin.mobile.data.repository.SchemeRepository
import com.fin.mobile.ui.notifications.NotificationsScreen
import com.fin.mobile.ui.notifications.NotificationsViewModel
import com.fin.mobile.ui.settings.SettingsScreen
import com.fin.mobile.ui.settings.SettingsViewModel
import com.fin.mobile.ui.applications.ApplicationDetailScreen
import com.fin.mobile.ui.applications.ApplicationDetailViewModel
import com.fin.mobile.ui.applications.ApplySchemeScreen
import com.fin.mobile.ui.applications.ApplySchemeViewModel
import com.fin.mobile.ui.applications.MyApplicationsScreen
import com.fin.mobile.ui.applications.MyApplicationsViewModel
import com.fin.mobile.ui.auth.AuthState
import com.fin.mobile.ui.auth.AuthViewModel
import com.fin.mobile.ui.auth.LoginScreen
import com.fin.mobile.ui.auth.SignupScreen
import com.fin.mobile.ui.auth.SplashScreen
import com.fin.mobile.ui.auth.WelcomeScreen
import com.fin.mobile.ui.chat.CitizenChatScreen
import com.fin.mobile.ui.chat.CitizenChatViewModel
import com.fin.mobile.ui.dashboard.CitizenDashboardScreen
import com.fin.mobile.ui.dashboard.CitizenDashboardViewModel
import com.fin.mobile.ui.dashboard.ReviewerDashboardBoundary
import com.fin.mobile.ui.documents.DocumentVaultScreen
import com.fin.mobile.ui.documents.DocumentVaultViewModel
import com.fin.mobile.ui.reviewer.ReviewerApplicationDetailScreen
import com.fin.mobile.ui.reviewer.ReviewerApplicationDetailViewModel
import com.fin.mobile.ui.reviewer.ReviewerDashboardScreen
import com.fin.mobile.ui.reviewer.ReviewerDashboardViewModel
import com.fin.mobile.ui.schemes.SchemeCatalogueScreen
import com.fin.mobile.ui.schemes.SchemeCatalogueViewModel
import com.fin.mobile.ui.schemes.SchemeDetailScreen
import com.fin.mobile.ui.schemes.SchemeDetailViewModel

object NavDestinations {
    const val SPLASH = "splash"
    const val WELCOME = "welcome"
    const val LOGIN = "login"
    const val SIGNUP = "signup"
    const val CITIZEN_DASHBOARD = "citizen_dashboard"
    const val REVIEWER_DASHBOARD = "reviewer_dashboard"
    const val SCHEME_CATALOGUE = "scheme_catalogue"
    const val SCHEME_DETAIL = "scheme_detail/{schemeId}"
    const val DOCUMENT_VAULT = "document_vault"
    const val MY_APPLICATIONS = "my_applications"
    const val APPLICATION_DETAILS = "application_details/{applicationId}"
    const val APPLY_SCHEME = "apply_scheme/{schemeId}"
    const val CITIZEN_CHAT = "citizen_chat"
    const val REVIEWER_APPLICATION_DETAIL = "reviewer_application_detail/{applicationId}"
    const val NOTIFICATIONS = "notifications"
    const val SETTINGS = "settings"

    fun schemeDetail(schemeId: String): String = "scheme_detail/$schemeId"
    fun applicationDetails(applicationId: String): String = "application_details/$applicationId"
    fun applyScheme(schemeId: String): String = "apply_scheme/$schemeId"
    fun reviewerApplicationDetail(applicationId: String): String = "reviewer_application_detail/$applicationId"
}

@Composable
fun AppNavigation(
    authViewModel: AuthViewModel,
    authRepository: AuthRepository? = null,
    dashboardRepository: DashboardRepository? = null,
    schemeRepository: SchemeRepository? = null,
    documentRepository: DocumentRepository? = null,
    applicationRepository: ApplicationRepository? = null,
    chatRepository: ChatRepository? = null,
    reviewerRepository: ReviewerRepository? = null,
    notificationRepository: NotificationRepository? = null,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val authState by authViewModel.authState.collectAsStateWithLifecycle()

    NavHost(
        navController = navController,
        startDestination = NavDestinations.SPLASH,
        modifier = modifier
    ) {
        composable(NavDestinations.SPLASH) {
            SplashScreen()

            LaunchedEffect(authState) {
                when (val state = authState) {
                    is AuthState.Authenticated -> {
                        val destination = if (state.role.equals("reviewer", ignoreCase = true)) {
                            NavDestinations.REVIEWER_DASHBOARD
                        } else {
                            NavDestinations.CITIZEN_DASHBOARD
                        }
                        navController.navigate(destination) {
                            popUpTo(NavDestinations.SPLASH) { inclusive = true }
                        }
                    }
                    is AuthState.Unauthenticated -> {
                        navController.navigate(NavDestinations.WELCOME) {
                            popUpTo(NavDestinations.SPLASH) { inclusive = true }
                        }
                    }
                    is AuthState.CheckingSession, is AuthState.Loading -> {
                        // Remain on splash while verifying session
                    }
                    is AuthState.Error -> {
                        navController.navigate(NavDestinations.LOGIN) {
                            popUpTo(NavDestinations.SPLASH) { inclusive = true }
                        }
                    }
                }
            }
        }

        composable(NavDestinations.WELCOME) {
            WelcomeScreen(
                onNavigateToLogin = {
                    navController.navigate(NavDestinations.LOGIN)
                },
                onNavigateToSignup = {
                    navController.navigate(NavDestinations.SIGNUP)
                }
            )
        }

        composable(NavDestinations.LOGIN) {
            LoginScreen(
                viewModel = authViewModel,
                onNavigateToSignup = {
                    navController.navigate(NavDestinations.SIGNUP)
                },
                onLoginSuccess = { role ->
                    val destination = if (role.equals("reviewer", ignoreCase = true)) {
                        NavDestinations.REVIEWER_DASHBOARD
                    } else {
                        NavDestinations.CITIZEN_DASHBOARD
                    }
                    navController.navigate(destination) {
                        popUpTo(NavDestinations.WELCOME) { inclusive = true }
                    }
                }
            )
        }

        composable(NavDestinations.SIGNUP) {
            SignupScreen(
                viewModel = authViewModel,
                onNavigateToLogin = {
                    navController.popBackStack()
                },
                onSignupSuccess = { role ->
                    val destination = if (role.equals("reviewer", ignoreCase = true)) {
                        NavDestinations.REVIEWER_DASHBOARD
                    } else {
                        NavDestinations.CITIZEN_DASHBOARD
                    }
                    navController.navigate(destination) {
                        popUpTo(NavDestinations.WELCOME) { inclusive = true }
                    }
                }
            )
        }

        composable(NavDestinations.CITIZEN_DASHBOARD) {
            val user = (authState as? AuthState.Authenticated)?.user

            if (dashboardRepository != null) {
                val dashboardViewModel: CitizenDashboardViewModel = viewModel(
                    factory = CitizenDashboardViewModel.provideFactory(dashboardRepository)
                )
                CitizenDashboardScreen(
                    viewModel = dashboardViewModel,
                    user = user,
                    onNavigateToCatalogue = {
                        navController.navigate(NavDestinations.SCHEME_CATALOGUE)
                    },
                    onNavigateToSchemeDetails = { schemeId ->
                        navController.navigate(NavDestinations.schemeDetail(schemeId))
                    },
                    onNavigateToDocuments = {
                        navController.navigate(NavDestinations.DOCUMENT_VAULT)
                    },
                    onNavigateToApplications = {
                        navController.navigate(NavDestinations.MY_APPLICATIONS)
                    },
                    onNavigateToAssistant = {
                        navController.navigate(NavDestinations.CITIZEN_CHAT)
                    },
                    onNavigateToNotifications = {
                        navController.navigate(NavDestinations.NOTIFICATIONS)
                    },
                    onNavigateToSettings = {
                        navController.navigate(NavDestinations.SETTINGS)
                    },
                    onSignOut = {
                        authViewModel.logout {
                            navController.navigate(NavDestinations.WELCOME) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                )
            } else {
                com.fin.mobile.ui.dashboard.CitizenDashboardBoundary(
                    user = user,
                    onSignOut = {
                        authViewModel.logout {
                            navController.navigate(NavDestinations.WELCOME) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                )
            }
        }

        composable(NavDestinations.REVIEWER_DASHBOARD) {
            val auth = authState as? AuthState.Authenticated
            val isReviewer = auth?.role?.equals("reviewer", ignoreCase = true) == true

            if (isReviewer && reviewerRepository != null) {
                val reviewerViewModel: ReviewerDashboardViewModel = viewModel(
                    factory = ReviewerDashboardViewModel.provideFactory(reviewerRepository)
                )
                ReviewerDashboardScreen(
                    viewModel = reviewerViewModel,
                    user = auth?.user,
                    onNavigateToApplicationDetails = { appId ->
                        navController.navigate(NavDestinations.reviewerApplicationDetail(appId))
                    },
                    onNavigateToNotifications = {
                        navController.navigate(NavDestinations.NOTIFICATIONS)
                    },
                    onNavigateToSettings = {
                        navController.navigate(NavDestinations.SETTINGS)
                    },
                    onSignOut = {
                        authViewModel.logout {
                            navController.navigate(NavDestinations.WELCOME) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                )
            } else {
                ReviewerDashboardBoundary(
                    user = auth?.user,
                    onSignOut = {
                        authViewModel.logout {
                            navController.navigate(NavDestinations.WELCOME) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                )
            }
        }

        composable(NavDestinations.SCHEME_CATALOGUE) {
            if (schemeRepository != null) {
                val catalogueViewModel: SchemeCatalogueViewModel = viewModel(
                    factory = SchemeCatalogueViewModel.provideFactory(schemeRepository)
                )
                SchemeCatalogueScreen(
                    viewModel = catalogueViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToSchemeDetails = { schemeId ->
                        navController.navigate(NavDestinations.schemeDetail(schemeId))
                    }
                )
            }
        }

        composable(
            route = NavDestinations.SCHEME_DETAIL,
            arguments = listOf(
                navArgument("schemeId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val schemeId = backStackEntry.arguments?.getString("schemeId") ?: ""
            if (schemeRepository != null && schemeId.isNotBlank()) {
                val detailViewModel: SchemeDetailViewModel = viewModel(
                    key = "detail_$schemeId",
                    factory = SchemeDetailViewModel.provideFactory(schemeRepository, schemeId)
                )
                SchemeDetailScreen(
                    viewModel = detailViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onApply = { targetSchemeId ->
                        navController.navigate(NavDestinations.applyScheme(targetSchemeId))
                    }
                )
            }
        }

        composable(NavDestinations.DOCUMENT_VAULT) {
            if (documentRepository != null) {
                val documentViewModel: DocumentVaultViewModel = viewModel(
                    factory = DocumentVaultViewModel.provideFactory(documentRepository)
                )
                DocumentVaultScreen(
                    viewModel = documentViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }

        composable(NavDestinations.MY_APPLICATIONS) {
            if (applicationRepository != null) {
                val applicationsViewModel: MyApplicationsViewModel = viewModel(
                    factory = MyApplicationsViewModel.provideFactory(applicationRepository)
                )
                MyApplicationsScreen(
                    viewModel = applicationsViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToDetail = { appId ->
                        navController.navigate(NavDestinations.applicationDetails(appId))
                    },
                    onExploreSchemes = {
                        navController.navigate(NavDestinations.SCHEME_CATALOGUE)
                    }
                )
            }
        }

        composable(
            route = NavDestinations.APPLICATION_DETAILS,
            arguments = listOf(
                navArgument("applicationId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val appId = backStackEntry.arguments?.getString("applicationId") ?: ""
            if (applicationRepository != null && appId.isNotBlank()) {
                val detailViewModel: ApplicationDetailViewModel = viewModel(
                    key = "app_detail_$appId",
                    factory = ApplicationDetailViewModel.provideFactory(appId, applicationRepository)
                )
                ApplicationDetailScreen(
                    viewModel = detailViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }

        composable(
            route = NavDestinations.APPLY_SCHEME,
            arguments = listOf(
                navArgument("schemeId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val schemeId = backStackEntry.arguments?.getString("schemeId") ?: ""
            if (applicationRepository != null && schemeRepository != null && documentRepository != null && schemeId.isNotBlank()) {
                val applyViewModel: ApplySchemeViewModel = viewModel(
                    key = "apply_$schemeId",
                    factory = ApplySchemeViewModel.provideFactory(
                        schemeId = schemeId,
                        applicationRepository = applicationRepository,
                        schemeRepository = schemeRepository,
                        documentRepository = documentRepository
                    )
                )
                ApplySchemeScreen(
                    viewModel = applyViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToVault = { navController.navigate(NavDestinations.DOCUMENT_VAULT) },
                    onNavigateToApplicationDetail = { appId ->
                        navController.navigate(NavDestinations.applicationDetails(appId)) {
                            popUpTo(NavDestinations.SCHEME_CATALOGUE)
                        }
                    },
                    onNavigateToMyApplications = {
                        navController.navigate(NavDestinations.MY_APPLICATIONS) {
                            popUpTo(NavDestinations.SCHEME_CATALOGUE)
                        }
                    }
                )
            }
        }

        composable(NavDestinations.CITIZEN_CHAT) {
            if (chatRepository != null) {
                val chatViewModel: CitizenChatViewModel = viewModel(
                    factory = CitizenChatViewModel.provideFactory(chatRepository)
                )
                CitizenChatScreen(
                    viewModel = chatViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToSchemeDetails = { schemeId ->
                        navController.navigate(NavDestinations.schemeDetail(schemeId))
                    },
                    onNavigateToDocuments = {
                        navController.navigate(NavDestinations.DOCUMENT_VAULT)
                    },
                    onNavigateToApplications = {
                        navController.navigate(NavDestinations.MY_APPLICATIONS)
                    }
                )
            }
        }

        composable(
            route = NavDestinations.REVIEWER_APPLICATION_DETAIL,
            arguments = listOf(
                navArgument("applicationId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val appId = backStackEntry.arguments?.getString("applicationId") ?: ""
            val auth = authState as? AuthState.Authenticated
            val isReviewer = auth?.role?.equals("reviewer", ignoreCase = true) == true

            if (!isReviewer) {
                // Reject citizen access to reviewer screens
                navController.popBackStack()
            } else if (reviewerRepository != null && appId.isNotBlank()) {
                val reviewerDetailViewModel: ReviewerApplicationDetailViewModel = viewModel(
                    key = "rev_detail_$appId",
                    factory = ReviewerApplicationDetailViewModel.provideFactory(appId, reviewerRepository)
                )
                ReviewerApplicationDetailScreen(
                    viewModel = reviewerDetailViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }

        composable(NavDestinations.NOTIFICATIONS) {
            if (notificationRepository != null) {
                val notificationsViewModel: NotificationsViewModel = viewModel(
                    factory = NotificationsViewModel.provideFactory(notificationRepository)
                )
                NotificationsScreen(
                    viewModel = notificationsViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToApplicationDetail = { appId ->
                        navController.navigate(NavDestinations.applicationDetails(appId))
                    }
                )
            }
        }

        composable(NavDestinations.SETTINGS) {
            if (authRepository != null) {
                val settingsViewModel: SettingsViewModel = viewModel(
                    factory = SettingsViewModel.provideFactory(authRepository)
                )
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onSignOut = {
                        authViewModel.logout {
                            navController.navigate(NavDestinations.WELCOME) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                )
            }
        }
    }
}
