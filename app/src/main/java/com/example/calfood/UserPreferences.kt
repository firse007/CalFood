package com.example.calfood

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

class UserPreferences(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    fun loadProfile(): UserProfile? {
        val name = sharedPreferences.getString("name", null) ?: return null
        val weight = sharedPreferences.getFloat("weight", 0f)
        val height = sharedPreferences.getFloat("height", 0f)
        val age = sharedPreferences.getInt("age", 0)
        val gender = sharedPreferences.getString("gender", "ชาย") ?: "ชาย"
        return UserProfile(name, weight, height, age, gender)
    }

    fun saveProfile(profile: UserProfile) {
        sharedPreferences.edit().apply {
            putString("name", profile.name)
            putFloat("weight", profile.weight)
            putFloat("height", profile.height)
            putInt("age", profile.age)
            putString("gender", profile.gender)
            apply()
        }
    }

    fun clearProfile() {
        sharedPreferences.edit().clear().apply()
    }

    fun saveDailyRecord(record: DailyRecord) {
        val records = loadDailyRecords().toMutableList()
        // Replace if date exists, else add
        val index = records.indexOfFirst { it.date == record.date }
        if (index != -1) {
            records[index] = record
        } else {
            records.add(record)
        }
        
        val jsonArray = JSONArray()
        records.forEach {
            val obj = JSONObject()
            obj.put("date", it.date)
            obj.put("calories", it.calories)
            jsonArray.put(obj)
        }
        sharedPreferences.edit().putString("daily_records", jsonArray.toString()).apply()
    }

    fun loadDailyRecords(): List<DailyRecord> {
        val jsonString = sharedPreferences.getString("daily_records", null) ?: return emptyList()
        val records = mutableListOf<DailyRecord>()
        val jsonArray = JSONArray(jsonString)
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            records.add(DailyRecord(obj.getString("date"), obj.getInt("calories")))
        }
        return records.sortedBy { it.date }
    }
}
