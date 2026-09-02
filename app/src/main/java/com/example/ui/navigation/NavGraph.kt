package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.FeedScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ProcessorScreen
import com.example.viewmodel.AudioCleanerViewModel
import com.example.viewmodel.AudioCleanerViewModelFactory

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Processor : Screen("processor")
    object Feed : Screen("feed")
    object History : Screen("history")
    object Browser : Screen("browser")
    object Floating : Screen("floating")
}

@Composable
fun NavGraph(startDestination: String = Screen.Home.route) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val viewModel: AudioCleanerViewModel = viewModel(
        factory = AudioCleanerViewModelFactory(context)
    )

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToProcessor = { navController.navigate(Screen.Processor.route) },
                onNavigateToFeed = { navController.navigate(Screen.Feed.route) },
                onNavigateToHistory = { navController.navigate(Screen.History.route) },
                onNavigateToBrowser = { navController.navigate(Screen.Browser.route) },
                onNavigateToFloating = { navController.navigate(Screen.Floating.route) }
            )
        }
        composable(Screen.Processor.route) {
            ProcessorScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Feed.route) {
            FeedScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onSelectAndProcess = { video ->
                    viewModel.selectVideo(video)
                    navController.navigate(Screen.Processor.route)
                }
            )
        }
        composable(Screen.History.route) {
            HistoryScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Browser.route) {
            com.example.ui.screens.TikTokBrowserScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Floating.route) {
            com.example.ui.screens.FloatingModeScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
