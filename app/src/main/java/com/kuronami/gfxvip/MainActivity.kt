package com.kuronami.gfxvip

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }
        setContent {
            MainAppScreen { toastMsg ->
                Toast.makeText(this, toastMsg, Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@Composable
fun MainAppScreen(showToast: (String) -> Unit) {
    val darkBg = Color(0xFF0D0F17)
    val cardBg = Color(0xFF131724)
    val neonBlue = Color(0xFF38BDF8)
    val goldColor = Color(0xFFFFB800)
    val purpleGradient = Brush.horizontalGradient(listOf(Color(0xFF2563EB), Color(0xFF9333EA)))

    var currentTab by remember { mutableStateOf(0) }

    // Quick Options States
    var floatingMenu by remember { mutableStateOf(false) }
    var darkMode by remember { mutableStateOf(true) }
    var updateBlocker by remember { mutableStateOf(true) }
    var fpsUnlock by remember { mutableStateOf(true) }

    // Custom Server States
    var aimValue by remember { mutableFloatStateOf(9f) }
    var bulletSpread by remember { mutableFloatStateOf(5f) }
    var selectedPreset by remember { mutableStateOf("MEDIUM") }

    Scaffold(
        containerColor = darkBg,
        bottomBar = {
            NavigationBar(containerColor = cardBg, tonalElevation = 8.dp) {
                NavigationBarItem(selected = currentTab == 0, onClick = { currentTab = 0 }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Home") })
                NavigationBarItem(selected = currentTab == 1, onClick = { currentTab = 1 }, icon = { Icon(Icons.Default.Folder, null) }, label = { Text("Files") })
                NavigationBarItem(selected = currentTab == 2, onClick = { currentTab = 2 }, icon = { Icon(Icons.Default.Build, null) }, label = { Text("Server") })
                NavigationBarItem(selected = currentTab == 3, onClick = { currentTab = 3 }, icon = { Icon(Icons.Default.Settings, null) }, label = { Text("Settings") })
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            when (currentTab) {
                0 -> HomeScreen(floatingMenu, darkMode, updateBlocker, fpsUnlock, purpleGradient, cardBg,
                    onToggle = { idx, state ->
                        when(idx) {
                            1 -> floatingMenu = state
                            2 -> darkMode = state
                            3 -> updateBlocker = state
                            4 -> fpsUnlock = state
                        }
                    },
                    onRun = { showToast("Applied 120 FPS & Optimized Configuration") }
                )
                1 -> FilesScreen(cardBg, neonBlue) { showToast("Downloaded and applied config!") }
                2 -> CustomServerScreen(goldColor, aimValue, bulletSpread, selectedPreset,
                    onAimChange = { aimValue = it },
                    onSpreadChange = { bulletSpread = it },
                    onPresetSelect = { selectedPreset = it },
                    onPush = {
                        val py = Python.getInstance()
                        val res = py.getModule("gfx_engine")
                            .callAttr("apply_custom_config", "{\"aim_value\":$aimValue,\"bullet_spread\":$bulletSpread,\"preset\":\"$selectedPreset\",\"no_recoil_guns\":[\"M416\",\"SCAR\"]}")
                            .toString()
                        showToast(res)
                    }
                )
                3 -> SettingsScreen(cardBg) { showToast("Permission Updated") }
            }
        }
    }
}

@Composable
fun HomeScreen(floating: Boolean, dark: Boolean, blocker: Boolean, fps: Boolean, gradient: Brush, cardBg: Color, onToggle: (Int, Boolean) -> Unit, onRun: () -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(50.dp).clip(CircleShape).background(Color(0xFF1E293B)).border(1.dp, Color(0xFF38BDF8), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("K", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("KURONAMI GFX.", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    Text("YOUR GAME, YOUR CHOICE !", color = Color.Gray, fontSize = 11.sp)
                }
            }
            Box(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(Color(0xFF1E1B4B)).padding(horizontal = 12.dp, vertical = 6.dp)) {
                Text("• JOIN US", color = Color(0xFFA855F7), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("QUICK OPTIONS", color = Color(0xFF94A3B8), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = cardBg), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(14.dp)) {
                SettingItem("Floating Menu", "Quick access overlay in-game", Icons.Default.DesktopWindows, floating) { onToggle(1, it) }
                SettingItem("Dark Mode", "Force dark theme app-wide", Icons.Default.DarkMode, dark) { onToggle(2, it) }
                SettingItem("Update Blocker", "Prevent Play Store auto-update", Icons.Default.Cancel, blocker) { onToggle(3, it) }
                SettingItem("120 FPS Unlock", "Requires supported device", Icons.Default.FlashOn, fps) { onToggle(4, it) }
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = onRun,
            modifier = Modifier.fillMaxWidth().height(54.dp).background(gradient, RoundedCornerShape(27.dp)),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("RUN--GAME--NOW", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
        Spacer(modifier = Modifier.height(10.dp))
    }
}

@Composable
fun FilesScreen(cardBg: Color, neonBlue: Color, onDownload: () -> Unit) {
    Column {
        Text("FILES", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Surface(modifier = Modifier.fillMaxWidth(), color = cardBg, shape = RoundedCornerShape(12.dp)) {
            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.School, contentDescription = null, tint = neonBlue)
                Spacer(modifier = Modifier.width(10.dp))
                Text("Target: BGMI Mobile [IN]", color = Color.White, fontSize = 14.sp)
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = cardBg), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("VIP OPTIMIZED CONFIG PACK", color = neonBlue, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text("• 0% Frame Drop\n• Small Crosshair Fix\n• Ultra Smooth Graphics", color = Color.LightGray, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(14.dp))
                Button(onClick = onDownload, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = neonBlue)) {
                    Text("Download & Apply", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun CustomServerScreen(gold: Color, aim: Float, spread: Float, preset: String, onAimChange: (Float) -> Unit, onSpreadChange: (Float) -> Unit, onPresetSelect: (String) -> Unit, onPush: () -> Unit) {
    LazyColumn {
        item {
            Text("♦ KNIGHT CORE CUSTOM SERVER", color = gold, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text("Aim Value: ${aim.toInt()}", color = Color.White, fontSize = 14.sp)
            Slider(value = aim, onValueChange = onAimChange, valueRange = 1f..10f, steps = 8)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Bullet Spread: ${spread.toInt()}", color = Color.White, fontSize = 14.sp)
            Slider(value = spread, onValueChange = onSpreadChange, valueRange = 1f..10f, steps = 8)
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                listOf("SMALL", "MEDIUM", "YOUTUBER").forEach { p ->
                    OutlinedButton(
                        onClick = { onPresetSelect(p) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = if (preset == p) gold else Color.Gray)
                    ) {
                        Text(p)
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Button(onClick = onPush, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = gold)) {
                Text("PUSH & APPLY TO GAME", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SettingsScreen(cardBg: Color, onGrant: () -> Unit) {
    Column {
        Text("SETTINGS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = cardBg), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(14.dp)) {
                PermissionItem("Setup Shizuku Permission", "Granted", Icons.Default.Security)
                PermissionItem("Setup VPN Permission", "Granted", Icons.Default.Lock)
                PermissionItem("Setup File Access", "Granted", Icons.Default.Folder)
                PermissionItem("Disable Battery Optimization", "Whitelisted", Icons.Default.BatteryChargingFull)
            }
        }
    }
}

@Composable
fun SettingItem(title: String, desc: String, icon: ImageVector, state: Boolean, onChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(desc, color = Color.Gray, fontSize = 11.sp)
        }
        Switch(checked = state, onCheckedChange = onChange)
    }
}

@Composable
fun PermissionItem(title: String, status: String, icon: ImageVector) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text("• $status", color = Color(0xFF22C55E), fontSize = 11.sp)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
    }
}
