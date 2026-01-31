package com.example.calfood

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
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
    advice: String,
    isAnalyzing: Boolean,
    aiScanResult: Food?,
    selectedFoods: List<Food>,
    onAddFood: (Food) -> Unit,
    onRemoveFood: (Food) -> Unit,
    onClearAll: () -> Unit,
    onEditProfile: () -> Unit,
    onNavigateToSummary: () -> Unit,
    onAnalyzeImage: (Bitmap) -> Unit,
    onClearScan: () -> Unit
) {
    val context = LocalContext.current
    
    // Launcher สำหรับถ่ายรูป
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            onAnalyzeImage(bitmap)
        }
    }

    // Launcher สำหรับเลือกรูปจาก Gallery
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val bitmap = if (Build.VERSION.SDK_INT < 28) {
                MediaStore.Images.Media.getBitmap(context.contentResolver, it)
            } else {
                val source = ImageDecoder.createSource(context.contentResolver, it)
                ImageDecoder.decodeBitmap(source)
            }
            onAnalyzeImage(bitmap)
        }
    }

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
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                // ปุ่ม Gallery
                SmallFloatingActionButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(Icons.Default.List, contentDescription = "Gallery")
                }
                // ปุ่มถ่ายรูป
                FloatingActionButton(
                    onClick = { cameraLauncher.launch() },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Search, contentDescription = "AI Scan")
                }
            }
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
            
            // ส่วนสรุปแคลอรี่
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
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // ส่วนคำแนะนำ (Advice)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Notifications, 
                            contentDescription = "Advice",
                            tint = if (totalCalories > dailyLimit) Color.Red else Color(0xFF2E7D32),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = advice,
                            fontSize = 13.sp,
                            fontStyle = FontStyle.Italic,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // แสดงผลการสแกน AI
            AnimatedVisibility(visible = isAnalyzing || aiScanResult != null) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isAnalyzing) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("กำลังวิเคราะห์อาหารด้วย AI...")
                        } else if (aiScanResult != null) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32))
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(aiScanResult.name, fontWeight = FontWeight.Bold)
                                Text("${aiScanResult.calories} kcal")
                            }
                            Button(onClick = { onAddFood(aiScanResult) }) {
                                Text("เพิ่ม")
                            }
                            IconButton(onClick = onClearScan) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
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
                
                Text("รายละเอียดและคำแนะนำรายวัน", fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(dailyRecords.reversed()) { record ->
                        val ratio = record.calories.toFloat() / dailyLimit.toFloat()
                        val (advice, color) = when {
                            record.calories == 0 -> "ไม่มีการบันทึกข้อมูล" to Color.Gray
                            ratio <= 1.0 -> "ทำได้ดีมาก! ควบคุมแคลอรี่ได้ตามเป้าหมาย" to Color(0xFF2E7D32)
                            else -> "ทานเกินเป้าหมาย! พยายามออกกำลังกายเพิ่มนะครับ" to Color.Red
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            ListItem(
                                headlineContent = { Text(record.date, fontWeight = FontWeight.Bold) },
                                supportingContent = { 
                                    Column {
                                        Text("แคลอรี่รวม: ${record.calories} kcal")
                                        Text(text = advice, color = color, fontSize = 12.sp, fontStyle = FontStyle.Italic)
                                    }
                                },
                                trailingContent = { 
                                    Text(
                                        if (ratio > 1.0) "เกินเกณฑ์" else "ปกติ",
                                        color = if (ratio > 1.0) Color.Red else Color(0xFF2E7D32),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CalorieChart(records: List<DailyRecord>, dailyLimit: Int) {
    val maxCalInRecords = records.maxOfOrNull { it.calories } ?: 0
    val maxY = maxOf(maxCalInRecords, dailyLimit).toFloat() * 1.3f
    
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
            .height(280.dp)
            .background(Color.White, RoundedCornerShape(12.dp))
            .padding(top = 32.dp, start = 16.dp, end = 16.dp, bottom = 48.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val spacing = width / (records.size + 1)
            
            // วาดเส้น Daily Limit
            val limitY = height - (dailyLimit.toFloat() / maxY * height)
            drawLine(
                color = Color.Red.copy(alpha = 0.5f),
                start = Offset(0f, limitY),
                end = Offset(width, limitY),
                strokeWidth = 1.dp.toPx()
            )

            records.forEachIndexed { index, record ->
                val barHeight = (record.calories.toFloat() / maxY) * height * animateProgress
                val x = spacing * (index + 1)
                val barWidth = 24.dp.toPx()
                
                // วาดแท่งกราฟ
                drawRect(
                    color = if (record.calories > dailyLimit) Color(0xFFFF8A80) else Color(0xFF81C784),
                    topLeft = Offset(x - barWidth / 2, height - barHeight),
                    size = Size(barWidth, barHeight)
                )

                // วาดตัวเลขแคลอรี่บนแท่ง (ใช้ Android Native Canvas เพื่อวาด Text)
                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.BLACK
                        textSize = 10.sp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                    if (animateProgress > 0.9f) {
                        drawText(
                            "${record.calories}",
                            x,
                            height - barHeight - 8.dp.toPx(),
                            paint
                        )
                    }
                }

                // วาดวันที่ใต้แท่ง
                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.GRAY
                        textSize = 10.sp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                    drawText(
                        record.date,
                        x,
                        height + 20.dp.toPx(),
                        paint
                    )
                }
            }
        }
    }
}
