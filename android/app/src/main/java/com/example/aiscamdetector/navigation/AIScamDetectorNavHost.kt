package com.example.aiscamdetector.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.aiscamdetector.data.ServiceLocator
import com.example.aiscamdetector.ui.screens.HistoryScreen
import com.example.aiscamdetector.ui.screens.LoginScreen
import com.example.aiscamdetector.ui.screens.ScanScreen
import com.example.aiscamdetector.ui.screens.SignupScreen
import com.example.aiscamdetector.ui.viewmodel.AuthViewModel
import com.example.aiscamdetector.ui.viewmodel.HistoryViewModel
import com.example.aiscamdetector.ui.viewmodel.ScanViewModel

object Routes {
    const val LOGIN = "login"
    const val SIGNUP = "signup"
    const val SCAN = "scan"
    const val HISTORY = "history"
}

@Composable
fun AIScamDetectorNavHost(
    navController: NavHostController = rememberNavController()
) {
    NavHost(navController = navController, startDestination = Routes.LOGIN) {

        composable(Routes.LOGIN) {
            val authViewModel: AuthViewModel = viewModel()
            LoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = {
                    navController.navigate(Routes.SCAN) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToSignup = { navController.navigate(Routes.SIGNUP) }
            )
        }

        composable(Routes.SIGNUP) {
            val authViewModel: AuthViewModel = viewModel()
            SignupScreen(
                viewModel = authViewModel,
                onAccountCreated = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.SIGNUP) { inclusive = true }
                    }
                },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }

        composable(Routes.SCAN) {
            val scanViewModel: ScanViewModel = viewModel(
                factory = ScanViewModel.factory(ServiceLocator.scanRepository)
            )
            ScanScreen(
                viewModel = scanViewModel,
                onOpenHistory = { navController.navigate(Routes.HISTORY) },
                onLogOut = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.HISTORY) {
            val historyViewModel: HistoryViewModel = viewModel(
                factory = HistoryViewModel.factory(ServiceLocator.historyRepository)
            )
            HistoryScreen(
                viewModel = historyViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
