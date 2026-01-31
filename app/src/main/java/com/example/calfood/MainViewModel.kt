package com.example.calfood

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class AppScreen {
    TRACKER, SUMMARY
}

class MainViewModel(private val userPrefs: UserPreferences) : ViewModel() {

    // คำเตือน: กรุณาสร้าง API Key ใหม่และห้ามแชร์ให้ใครนะครับ
    private val GEMINI_API_KEY = "AIzaSyCwQMW4coCLRVzaL9jmLTaFavhEltrbP1I"

    private val generativeModel = GenerativeModel(
        // ลองเปลี่ยนเป็น gemini-1.5-flash-latest ซึ่งมักจะเสถียรกว่าบน v1beta
        modelName = "gemini-1.5-flash-latest",
        apiKey = GEMINI_API_KEY
    )

    var userProfile by mutableStateOf(userPrefs.loadProfile())
        private set

    var currentScreen by mutableStateOf(AppScreen.TRACKER)
        private set

    val selectedFoods = mutableStateListOf<Food>()

    var dailyRecords by mutableStateOf(userPrefs.loadDailyRecords())
        private set

    var isAnalyzing by mutableStateOf(false)
        private set
    var aiScanResult by mutableStateOf<Food?>(null)
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
        aiScanResult = null
    }

    fun removeFood(food: Food) {
        selectedFoods.remove(food)
        saveCurrentDayRecord()
    }

    fun clearSelectedFoods() {
        selectedFoods.clear()
        saveCurrentDayRecord()
    }

    fun analyzeFoodImage(bitmap: Bitmap) {
        viewModelScope.launch {
            isAnalyzing = true
            aiScanResult = null
            
            try {
                // ย่อขนาดรูปภาพก่อนส่ง (ช่วยลดปัญหา Error และทำงานเร็วขึ้น)
                val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 512, 512, true)
                
                val prompt = "Analyze this food image. Provide ONLY the food name in Thai and estimated calories in this format: 'Name, Calories' (e.g., ข้าวมันไก่, 590)"
                
                val inputContent = content {
                    image(resizedBitmap)
                    text(prompt)
                }

                val response = generativeModel.generateContent(inputContent)
                val resultText = response.text ?: ""
                
                if (resultText.contains(",")) {
                    val parts = resultText.split(",")
                    val name = parts[0].trim()
                    val calories = parts[1].filter { it.isDigit() }.toIntOrNull() ?: 0
                    
                    aiScanResult = Food(
                        id = (100..999).random(),
                        name = "$name (AI)",
                        calories = calories
                    )
                } else {
                    aiScanResult = Food(0, "AI ผลลัพธ์: $resultText", 0)
                }
            } catch (e: Exception) {
                // จัดการ Error ให้กระชับขึ้น
                val errorMsg = if (e.message?.contains("404") == true) "ไม่พบ Model หรือ Region ไม่รองรับ" else e.message
                aiScanResult = Food(0, "ข้อผิดพลาด: $errorMsg", 0)
            } finally {
                isAnalyzing = false
            }
        }
    }

    fun clearScanResult() {
        aiScanResult = null
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
