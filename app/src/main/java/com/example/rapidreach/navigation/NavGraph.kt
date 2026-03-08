package com.example.rapidreach.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.example.rapidreach.screens.LoginScreen
import com.example.rapidreach.screens.SignupScreen
import com.example.rapidreach.screens.dashboard.DashboardScreen
import com.example.rapidreach.screens.profile.ProfileScreen
import com.example.rapidreach.screens.helpline.HelplineScreen
import com.example.rapidreach.screens.map.NearbyMapScreen
import com.example.rapidreach.screens.fakecall.FakeCallScreen
import com.example.rapidreach.screens.liveshare.LiveShareScreen
import com.example.rapidreach.viewmodel.AuthViewModel

@Composable
fun AppNavGraph(navController: NavHostController) {
    val authViewModel: AuthViewModel = viewModel()
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()

    NavHost(
        navController = navController,
        startDestination = if (isLoggedIn) Routes.DASHBOARD else Routes.LOGIN
    ) {

        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginClick = { navController.navigate(Routes.DASHBOARD) },
                onSignupClick = { navController.navigate(Routes.SIGNUP) }
            )
        }

        composable(Routes.SIGNUP) {
            SignupScreen(
                authViewModel = authViewModel,
                onSignupSuccess = {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onBackToLogin = { navController.popBackStack() }
            )
        }

        composable(Routes.DASHBOARD) {
            DashboardScreen(
                authViewModel = authViewModel,
                onNavigateToProfile = { navController.navigate(Routes.PROFILE) },
                onNavigateToHelpline = { navController.navigate(Routes.HELPLINE) },
                onNavigateToMap = { navController.navigate(Routes.MAP) },
                onNavigateToFakeCall = { navController.navigate(Routes.FAKE_CALL) },
                onNavigateToLiveShare = { navController.navigate(Routes.LIVE_SHARE) },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.PROFILE) {
            ProfileScreen(
                currentUser = currentUser,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.HELPLINE) {
            HelplineScreen(
                currentUser = currentUser,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.MAP) {
            NearbyMapScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.FAKE_CALL) {
            FakeCallScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.LIVE_SHARE) {
            LiveShareScreen(onBack = { navController.popBackStack() })
        }
    }
}