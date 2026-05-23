package com.pocketagent.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pocketagent.app.ui.screens.HomeScreen
import com.pocketagent.app.ui.screens.SetupScreen
import com.pocketagent.app.ui.screens.TaskScreen
import com.pocketagent.app.ui.screens.TerminalScreen

object Routes {
    const val HOME = "home"
    const val SETUP = "setup"
    const val TASK = "task"
    const val TERMINAL = "terminal"
}

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Routes.HOME
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onNavigateToTask = { navController.navigate(Routes.TASK) },
                onNavigateToTerminal = { navController.navigate(Routes.TERMINAL) }
            )
        }
        composable(Routes.SETUP) {
            SetupScreen(
                onSetupComplete = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.SETUP) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.TASK) {
            TaskScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Routes.TERMINAL) {
            TerminalScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}