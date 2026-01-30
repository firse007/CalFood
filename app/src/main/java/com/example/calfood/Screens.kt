package com.example.calfood

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// Extension สำหรับทำปุ่มกดแล้วยุบ (Bounce Effect) พร้อมรองรับ onClick
fun Modifier.bounceClick(onClick: () -> Unit) = composed {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "bounce"
    )
    
    LaunchedEffect(isPressed) {
        if (isPressed) {
            delay(100)
            isPressed = false
        }
    }

    this.graphicsLayer(scaleX = scale, scaleY = scale)
        .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = LocalIndication.current,
            onClick = {
                onClick()
                isPressed = true
            }
        )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupScreen(onProfileSaved: (UserProfile) -> Unit) {
    var name by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("ชาย") }

    Scaffold(
        topBar = { TopAppBar(title = { Text("ข้อมูลส่วนตัว") }) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("กรุณากรอกข้อมูลเพื่อคำนวณแคลอรี่ที่เหมาะสม", fontSize = 16.sp)
            
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("ชื่อเล่น") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = { Text("น้ำหนัก (กก.)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = height,
                    onValueChange = { height = it },
                    label = { Text("ส่วนสูง (ซม.)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = age,
                onValueChange = { age = it },
                label = { Text("อายุ (ปี)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Text("เพศ")
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = gender == "ชาย", onClick = { gender = "ชาย" })
                Text("ชาย")
                Spacer(modifier = Modifier.width(16.dp))
                RadioButton(selected = gender == "หญิง", onClick = { gender = "หญิง" })
                Text("หญิง")
            }

            Button(
                onClick = {
                    if (name.isNotEmpty() && weight.isNotEmpty() && height.isNotEmpty() && age.isNotEmpty()) {
                        onProfileSaved(UserProfile(
                            name = name,
                            weight = weight.toFloatOrNull() ?: 0f,
                            height = height.toFloatOrNull() ?: 0f,
                            age = age.toIntOrNull() ?: 0,
                            gender = gender
                        ))
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            ) {
                Text("บันทึกและเริ่มใช้งาน")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalorieTrackerApp(
    profile: UserProfile,
    totalCalories: Int,
    dailyLimit: Int,
    selectedFoods: List<Food>,
    onAddFood: (Food) -> Unit,
    onRemoveFood: (Food) -> Unit,
    onClearAll: () -> Unit,
    onEditProfile: () -> Unit,
    onNavigateToSummary: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("สวัสดีคุณ ${profile.name}") },
                actions = {
                    IconButton(onClick = onNavigateToSummary) {
                        Icon(Icons.Default.Info, contentDescription = "Summary")
                    }
                    TextButton(onClick = onEditProfile) {
                        Text("แก้ไข", color = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            val cardColor by animateColorAsState(
                targetValue = if (totalCalories > dailyLimit) Color(0xFFFFCDD2) else Color(0xFFC8E6C9),
                label = "cardColor"
            )
            
            Card(
                modifier = Modifier.fillMaxWidth().animateContentSize(),
                colors = CardDefaults.cardColors(containerColor = cardColor)
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "เป้าหมายแคลอรี่ของคุณ (TDEE)", fontSize = 14.sp)
                    Text(
                        text = "$totalCalories / $dailyLimit kcal",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (totalCalories > dailyLimit) Color.Red else Color(0xFF2E7D32)
                    )
                    if (totalCalories > dailyLimit) {
                        Text(text = "เกินเกณฑ์ที่ร่างกายต้องการต่อวันแล้ว!", color = Color.Red, fontWeight = FontWeight.Bold)
                    } else {
                        Text(text = "เหลืออีก ${dailyLimit - totalCalories} kcal", fontSize = 14.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "เลือกอาหารที่ทาน:", fontWeight = FontWeight.Bold)
            
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(foodList) { food ->
                    ListItem(
                        modifier = Modifier
                            .bounceClick { onAddFood(food) }
                            .padding(vertical = 2.dp),
                        headlineContent = { Text(food.name) },
                        supportingContent = { Text("${food.calories} kcal") },
                        trailingContent = {
                            Icon(
                                Icons.Default.Add, 
                                contentDescription = "Add",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    )
                    HorizontalDivider()
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "รายการวันนี้:", fontWeight = FontWeight.Bold)
            
            LazyColumn(modifier = Modifier.weight(0.7f)) {
                items(selectedFoods) { food ->
                    AnimatedVisibility(
                        visible = true,
                        enter = slideInHorizontally() + fadeIn(),
                        exit = slideOutHorizontally() + fadeOut()
                    ) {
                        ListItem(
                            headlineContent = { Text(food.name) },
                            supportingContent = { Text("${food.calories} kcal") },
                            trailingContent = {
                                IconButton(onClick = { onRemoveFood(food) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove")
                                }
                            }
                        )
                    }
                }
            }
            
            Button(
                onClick = onClearAll,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("ล้างรายการทั้งหมด")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
    dailyRecords: List<DailyRecord>,
    dailyLimit: Int,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("สรุปผลแคลอรี่") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Home, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("สถิติการกินย้อนหลัง", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))
            
            if (dailyRecords.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("ยังไม่มีข้อมูลสถิติ")
                }
            } else {
                CalorieChart(records = dailyRecords, dailyLimit = dailyLimit)
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Text("รายละเอียด", fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(dailyRecords.reversed()) { record ->
                        ListItem(
                            headlineContent = { Text(record.date) },
                            trailingContent = { 
                                Text(
                                    "${record.calories} kcal",
                                    color = if (record.calories > dailyLimit) Color.Red else Color(0xFF2E7D32)
                                )
                            }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
fun CalorieChart(records: List<DailyRecord>, dailyLimit: Int) {
    val maxCalInRecords = records.maxOfOrNull { it.calories } ?: 0
    val maxY = maxOf(maxCalInRecords, dailyLimit).toFloat() * 1.2f
    
    var animationPlayed by remember { mutableStateOf(false) }
    val animateProgress by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "chartAnimation"
    )
    
    LaunchedEffect(Unit) {
        animationPlayed = true
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .background(Color.White, RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val spacing = width / (records.size + 1)
            
            val limitY = height - (dailyLimit.toFloat() / maxY * height)
            drawLine(
                color = Color.Red.copy(alpha = 0.5f),
                start = Offset(0f, limitY),
                end = Offset(width, limitY),
                strokeWidth = 2.dp.toPx()
            )

            records.forEachIndexed { index, record ->
                val barHeight = (record.calories.toFloat() / maxY) * height * animateProgress
                val x = spacing * (index + 1)
                
                drawRect(
                    color = if (record.calories > dailyLimit) Color(0xFFFF8A80) else Color(0xFF81C784),
                    topLeft = Offset(x - 15.dp.toPx(), height - barHeight),
                    size = Size(30.dp.toPx(), barHeight)
                )
            }
        }
    }
    Text("เส้นสีแดงคือเป้าหมายประจำวัน ($dailyLimit kcal)", fontSize = 12.sp, color = Color.Gray)
}
