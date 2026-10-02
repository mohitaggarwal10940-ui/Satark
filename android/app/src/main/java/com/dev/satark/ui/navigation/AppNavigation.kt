package com.dev.satark.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dev.satark.ui.home.AnalysisUiState
import com.dev.satark.ui.home.HomeScreen
import com.dev.satark.ui.home.HomeScreenContent
import com.dev.satark.ui.home.HomeViewModel
import com.dev.satark.ui.input.InputScreen
import com.dev.satark.ui.preview.PreviewScreen
import com.dev.satark.ui.result.ResultScreen
import com.dev.satark.ui.theme.SatarkTheme

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Input : Screen("input")
    data object Preview : Screen("preview")
    data object Result : Screen("result")
}

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    viewModel: HomeViewModel = viewModel()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier,
        enterTransition = { fadeIn(animationSpec = tween(250)) },
        exitTransition = { fadeOut(animationSpec = tween(250)) },
        popEnterTransition = { fadeIn(animationSpec = tween(250)) },
        popExitTransition = { fadeOut(animationSpec = tween(250)) }
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToInput = {
                    navController.navigate(Screen.Input.route) {
                        launchSingleTop = true
                    }
                },
                onNavigateToPreview = {
                    navController.navigate(Screen.Preview.route) {
                        launchSingleTop = true
                    }
                },
                onNavigateToResult = {
                    navController.navigate(Screen.Result.route) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Screen.Input.route) {
            InputScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToPreview = {
                    navController.navigate(Screen.Preview.route) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Screen.Preview.route) {
            PreviewScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToResult = {
                    navController.navigate(Screen.Result.route) {
                        popUpTo(Screen.Home.route) {
                            inclusive = false
                        }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Screen.Result.route) {
            ResultScreen(
                viewModel = viewModel,
                onNavigateHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) {
                            inclusive = false
                        }
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AppNavigationPreview() {
    SatarkTheme {
        HomeScreenContent(
            uiState = AnalysisUiState(),
            snackbarHostState = remember { SnackbarHostState() },
            onUploadScreenshotClick = {},
            onTakePhotoClick = {},
            onPasteTextClick = {},
            onSampleScanClick = {}
        )
    }
}
