package com.example.calfood

data class Food(val id: Int, val name: String, val calories: Int)

data class DailyRecord(val date: String, val calories: Int)

data class UserProfile(
    val name: String = "",
    val weight: Float = 0f,
    val height: Float = 0f,
    val age: Int = 0,
    val gender: String = "ชาย"
)

val foodList = listOf(
    Food(1, "ข้าวกะเพราไก่ไข่ดาว", 550),
    Food(2, "ข้าวมันไก่", 590),
    Food(3, "ผัดไทยกุ้งสด", 480),
    Food(4, "ส้มตำไทย", 120),
    Food(5, "ก๋วยเตี๋ยวเรือ", 180),
    Food(6, "ข้าวขาหมู", 690),
    Food(7, "แกงเขียวหวานไก่", 240),
    Food(8, "สลัดผัก", 100),
    Food(9, "ชานมไข่มุก", 360),
    Food(10, "กาแฟดำ", 10)
)
