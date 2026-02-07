package com.example.calfood

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class AppScreen {
    TRACKER, SUMMARY
}

class MainViewModel(
    private val userPrefs: UserPreferences,
    private val foodDao: FoodDao
) : ViewModel() {

    var userProfile by mutableStateOf(userPrefs.loadProfile())
        private set

    var currentScreen by mutableStateOf(AppScreen.TRACKER)
        private set

    val selectedFoods = mutableStateListOf<Food>()

    // แปลง Flow จาก DB เป็น StateFlow เพื่อใช้ใน UI
    val availableFoods: StateFlow<List<Food>> = foodDao.getAllFoods()
        .map { entities ->
            entities.map { Food(it.id, it.name, it.calories) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

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

    val advice: String
        get() {
            val ratio = totalCalories.toFloat() / dailyLimit.toFloat()
            return when {
                totalCalories == 0 -> "เริ่มต้นวันใหม่ด้วยอาหารที่มีประโยชน์นะครับ!"
                ratio < 0.5 -> "คุณยังทานได้อีกเยอะเลย อย่าลืมทานให้ครบ 5 หมู่นะครับ"
                ratio < 0.8 -> "กำลังดีครับ! หากต้องการออกกำลังกาย แนะนำเดินเร็ว 30 นาที"
                ratio <= 1.0 -> "ใกล้ครบโควต้าแล้วครับ มื้อเย็นแนะนำเป็นสลัดหรือผลไม้เบาๆ"
                else -> "วันนี้ทานเกินเป้าหมายแล้ว! ควรออกกำลังกายเพื่อเผาผลาญ (เช่น วิ่ง 45 นาที หรือว่ายน้ำ)"
            }
        }

    init {
        // เพิ่มข้อมูลเริ่มต้นจากดิบ (Raw Data) หากยังไม่มีในฐานข้อมูล
        viewModelScope.launch {
            if (foodDao.getFoodCount() == 0) {
                rawFoodData.forEach { (name, calories) ->
                    foodDao.insertFood(FoodEntity(name = name, calories = calories))
                }
            }
        }
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

    fun addFoodToSelected(food: Food) {
        selectedFoods.add(food)
        saveCurrentDayRecord()
    }

    fun addNewFoodToDatabase(name: String, calories: Int) {
        viewModelScope.launch {
            foodDao.insertFood(FoodEntity(name = name, calories = calories))
        }
    }

    fun updateFoodInDatabase(food: Food) {
        viewModelScope.launch {
            foodDao.updateFood(FoodEntity(id = food.id, name = food.name, calories = food.calories))
        }
    }

    fun deleteFoodFromDatabase(food: Food) {
        viewModelScope.launch {
            foodDao.deleteFood(FoodEntity(id = food.id, name = food.name, calories = food.calories))
        }
    }

    fun removeFoodFromSelected(food: Food) {
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
