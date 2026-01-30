package com.example.calfood

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import java.text.SimpleDateFormat
import java.util.*

enum class AppScreen {
    TRACKER, SUMMARY
}

class MainViewModel(private val userPrefs: UserPreferences) : ViewModel() {

    var userProfile by mutableStateOf(userPrefs.loadProfile())
        private set

    var currentScreen by mutableStateOf(AppScreen.TRACKER)
        private set

    val selectedFoods = mutableStateListOf<Food>()

    var dailyRecords by mutableStateOf(userPrefs.loadDailyRecords())
        private set

    val totalCalories: Int
        get() = selectedFoods.sumOf { it.calories }

    val dailyLimit: Int
        get() {
            val profile = userProfile ?: return 2000
            val bmr = if (profile.gender == "ชาย") {
                (10 * profile.weight) + (6.25 * profile.height) - (5 * profile.age) + 5
            } else {
                (10 * profile.weight) + (6.25 * profile.height) - (5 * profile.age) - 161
            }
            return (bmr * 1.2).toInt()
        }

    fun saveProfile(profile: UserProfile) {
        userPrefs.saveProfile(profile)
        userProfile = profile
    }

    fun clearProfile() {
        userPrefs.clearProfile()
        userProfile = null
        selectedFoods.clear()
        currentScreen = AppScreen.TRACKER
    }

    fun addFood(food: Food) {
        selectedFoods.add(food)
        saveCurrentDayRecord()
    }

    fun removeFood(food: Food) {
        selectedFoods.remove(food)
        saveCurrentDayRecord()
    }

    fun clearSelectedFoods() {
        selectedFoods.clear()
        saveCurrentDayRecord()
    }

    private fun saveCurrentDayRecord() {
        val sdf = SimpleDateFormat("dd/MM", Locale.getDefault())
        val today = sdf.format(Date())
        val record = DailyRecord(today, totalCalories)
        userPrefs.saveDailyRecord(record)
        dailyRecords = userPrefs.loadDailyRecords()
    }

    fun navigateTo(screen: AppScreen) {
        currentScreen = screen
    }
}
