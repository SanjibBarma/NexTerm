package com.nexterm.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nexterm.app.ui.screens.main.MainScreen
import com.nexterm.app.ui.screens.terminal.TerminalScreen
import com.nexterm.app.ui.screens.files.FilesScreen
import com.nexterm.app.ui.screens.settings.SettingsScreen

sealed class Screen(val route: String) {
    object Main : Screen("main")

    object Terminal : Screen("terminal?command={command}") {
        const val baseRoute = "terminal"

        fun createRoute(command: String? = null): String {
            return if (command.isNullOrBlank()) {
                baseRoute
            } else {
                "$baseRoute?command=$command"
            }
        }
    }

    object Files : Screen("files")
    object Settings : Screen("settings")
}

@Composable
fun NexTermNavHost(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Main.route
    ) {
        composable(Screen.Main.route) {
            MainScreen(navController = navController)
        }
        composable(
            route = Screen.Terminal.route,
            arguments = listOf(
                navArgument("command") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val command = backStackEntry.arguments?.getString("command")

            TerminalScreen(
                initialCommand = command,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToFiles = { navController.navigate(Screen.Files.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }
        composable(Screen.Files.route) {
            FilesScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
