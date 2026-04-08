package com.baobao.fatloss.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.baobao.fatloss.FitAssistantApp
import com.baobao.fatloss.ui.aichat.AiChatScreen
import com.baobao.fatloss.ui.camera.CameraScreen
import com.baobao.fatloss.ui.home.HomeScreen
import com.baobao.fatloss.ui.meallog.MealLogScreen
import com.baobao.fatloss.ui.profile.ProfileScreen
import com.baobao.fatloss.ui.progress.ProgressScreen
import com.baobao.fatloss.ui.memory.MemoryScreen
import com.baobao.fatloss.ui.setup.ApiKeySetupScreen
import com.baobao.fatloss.viewmodel.*

@Composable
fun NavGraph(
    navController: NavHostController,
    paddingValues: androidx.compose.foundation.layout.PaddingValues,
    startDestination: String = Screen.Home.route,
) {
    val app = navController.context.applicationContext as FitAssistantApp
    val container = app.container
    val context = LocalContext.current

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = Modifier.padding(paddingValues),
    ) {
        composable(Screen.ApiKeySetup.route) {
            val vm: ApiKeySetupViewModel = viewModel(
                factory = ApiKeySetupViewModel.Factory(container.apiKeyStore, context)
            )
            ApiKeySetupScreen(
                viewModel = vm,
                onSaveSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.ApiKeySetup.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Home.route) {
            val vm: HomeViewModel = viewModel(
                factory = HomeViewModel.Factory(
                    container.userProfileRepo,
                    container.dailyLedgerRepo,
                    container.foodLogRepo,
                    container.weightRecordRepo,
                    container.aiRepo,
                    context
                )
            )
            HomeScreen(navController, vm)
        }
        composable(Screen.Camera.route) {
            val vm: CameraViewModel = viewModel(
                factory = CameraViewModel.Factory(
                    container.foodLogRepo,
                    container.dailyLedgerRepo,
                    container.aiRepo
                )
            )
            CameraScreen(navController, vm)
        }
        composable(Screen.MealLog.route) {
            val vm: MealLogViewModel = viewModel(
                factory = MealLogViewModel.Factory(
                    container.foodLogRepo,
                    container.dailyLedgerRepo,
                    container.userProfileRepo,
                    container.aiRepo,
                    context
                )
            )
            MealLogScreen(navController, vm)
        }
        composable(Screen.Progress.route) {
            val vm: ProgressViewModel = viewModel(
                factory = ProgressViewModel.Factory(
                    container.weightRecordRepo,
                    container.userProfileRepo,
                    container.dailyLedgerRepo
                )
            )
            ProgressScreen(navController, vm)
        }
        composable(Screen.AiChat.route) {
            val vm: AiChatViewModel = viewModel(
                factory = AiChatViewModel.Factory(
                    container.aiRepo,
                    container.foodLogRepo,
                    container.dailyLedgerRepo,
                    container.userProfileRepo
                )
            )
            // AiChat 不再有底部导航栏，直接使用完整 innerPadding
            AiChatScreen(navController, vm, modifier = Modifier.padding(paddingValues))
        }
        composable(Screen.Profile.route) {
            val vm: ProfileViewModel = viewModel(
                factory = ProfileViewModel.Factory(
                    container.userProfileRepo,
                    container.weightRecordRepo,
                    container.apiKeyStore
                )
            )
            ProfileScreen(navController, vm)
        }
        composable(Screen.MemoryManagement.route) {
            val vm: MemoryViewModel = viewModel(
                factory = MemoryViewModel.Factory(
                    container.userMemoryDao,
                    container.dailyNoteDao,
                    container.aiRepo
                )
            )
            MemoryScreen(navController, vm)
        }
    }
}
