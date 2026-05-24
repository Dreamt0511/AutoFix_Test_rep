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

/**
 * App 导航图 - Jetpack Navigation 路由绑定
 */
@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(navController)
        }
        composable("chat") {
            ChatScreen(navController)
        }
        composable("config") {
            ConfigScreen(navController)
        }
        composable("terminal") {
            TerminalScreen(navController)
        }
        composable("overlay") {
            OverlayScreen(navController)
        }
    }
}