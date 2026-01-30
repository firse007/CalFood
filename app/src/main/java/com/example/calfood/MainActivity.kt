package com.example.calfood

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.calfood.ui.theme.CalFoodTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CalFoodTheme {
                MainApp()
            }
        }
    }
}

@Composable
fun MainApp() {
    val context = LocalContext.current
    val viewModel: MainViewModel = viewModel(
        factory = MainViewModelFactory(UserPreferences(context))
    )

    val userProfile = viewModel.userProfile

    if (userProfile == null) {
        ProfileSetupScreen(onProfileSaved = viewModel::saveProfile)
    } else {
        // อนิเมชั่นตอนเปลี่ยนหน้าจอหลัก
        AnimatedContent(
            targetState = viewModel.currentScreen,
            transitionSpec = {
                if (targetState == AppScreen.SUMMARY) {
                    slideInHorizontally { it } + fadeIn() togetherWith
                            slideOutHorizontally { -it } + fadeOut()
                } else {
                    slideInHorizontally { -it } + fadeIn() togetherWith
                            slideOutHorizontally { it } + fadeOut()
                }.using(SizeTransform(clip = false))
            },
            label = "screenTransition"
        ) { screen ->
            when (screen) {
                AppScreen.TRACKER -> {
                    CalorieTrackerApp(
                        profile = userProfile,
                        totalCalories = viewModel.totalCalories,
                        dailyLimit = viewModel.dailyLimit,
                        selectedFoods = viewModel.selectedFoods,
                        onAddFood = viewModel::addFood,
                        onRemoveFood = viewModel::removeFood,
                        onClearAll = viewModel::clearSelectedFoods,
                        onEditProfile = viewModel::clearProfile,
                        onNavigateToSummary = { viewModel.navigateTo(AppScreen.SUMMARY) }
                    )
                }
                AppScreen.SUMMARY -> {
                    SummaryScreen(
                        dailyRecords = viewModel.dailyRecords,
                        dailyLimit = viewModel.dailyLimit,
                        onBack = { viewModel.navigateTo(AppScreen.TRACKER) }
                    )
                }
            }
        }
    }
}

class MainViewModelFactory(private val userPrefs: UserPreferences) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return MainViewModel(userPrefs) as T
    }
}
