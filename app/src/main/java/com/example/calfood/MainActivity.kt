package com.example.calfood

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Room
import com.example.calfood.ui.theme.CalFoodTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Database
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "calfood-database"
        ).fallbackToDestructiveMigration() // Simple for development
         .build()
        
        val foodDao = db.foodDao()

        enableEdgeToEdge()
        setContent {
            CalFoodTheme {
                MainApp(foodDao)
            }
        }
    }
}

@Composable
fun MainApp(foodDao: FoodDao) {
    val context = LocalContext.current
    val viewModel: MainViewModel = viewModel(
        factory = MainViewModelFactory(UserPreferences(context), foodDao)
    )

    val userProfile = viewModel.userProfile
    val availableFoods by viewModel.availableFoods.collectAsState()

    if (userProfile == null) {
        ProfileSetupScreen(onProfileSaved = viewModel::saveProfile)
    } else {
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
                        advice = viewModel.advice,
                        selectedFoods = viewModel.selectedFoods,
                        availableFoods = availableFoods,
                        onAddFood = viewModel::addFoodToSelected,
                        onAddNewFood = viewModel::addNewFoodToDatabase,
                        onUpdateFood = viewModel::updateFoodInDatabase,
                        onRemoveFood = viewModel::removeFoodFromSelected,
                        onDeleteFoodFromDb = viewModel::deleteFoodFromDatabase,
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

class MainViewModelFactory(
    private val userPrefs: UserPreferences,
    private val foodDao: FoodDao
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return MainViewModel(userPrefs, foodDao) as T
    }
}
