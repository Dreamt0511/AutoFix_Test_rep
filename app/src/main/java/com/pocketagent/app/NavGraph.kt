package com.pocketagent.app

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.pocketagent.app.ui.home.HomeScreen
import com.pocketagent.app.ui.chat.ChatScreen
import com.pocketagent.app.ui.config.ConfigScreen
import com.pocketagent.app.ui.terminal.TerminalScreen
import com.pocketagent.app.ui.overlay.OverlayScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Chat : Screen("chat")
    object Config : Screen("config")
    object Terminal : Screen("terminal")
    object Overlay : Screen("overlay")
}

@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(navController = navController)
        }
        composable(Screen.Chat.route) {
            ChatScreen(navController = navController)
        }
        composable(Screen.Config.route) {
            ConfigScreen(navController = navController)
        }
        composable(Screen.Terminal.route) {
            TerminalScreen(navController = navController)
        }
        composable(Screen.Overlay.route) {
            OverlayScreen(navController = navController)
        }
    }
}