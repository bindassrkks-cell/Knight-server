package com.kuronami.gfxvip

import android.os.Bundle
import android.os.Environment
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {
    private val client = OkHttpClient()
    // Replace this with your live Render URL
    val serverBaseUrl = "https://your-render-app.onrender.com"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }
        setContent {
            val scope = rememberCoroutineScope()
            MainAppScreen(
                onCompileAndDownload = {
                    scope.launch {
                        val msg = downloadAndInstallPak(serverBaseUrl)
                        Toast.makeText(this@MainActivity, msg, Toast.LENGTH_LONG).show()
                    }
                },
                showToast = { Toast.makeText(this@MainActivity, it, Toast.LENGTH_SHORT).show() }
            )
        }
    }

    private suspend fun downloadAndInstallPak(baseUrl: String): String = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder().url("$baseUrl/api/download-pak").build()
            val res = client.newCall(req).execute()
            if (!res.isSuccessful) return@withContext "Download Failed: Code ${res.code}"

            val body = res.body ?: return@withContext "Empty response body"

            // Target Paths: Internal game puffer_temp & storage RJTOOL/RESULT_PAK
            val pufferDir = File(
                Environment.getExternalStorageDirectory(),
                "Android/data/com.pubg.imobile/files/UE4Game/ShadowTrackerExtra/ShadowTrackerExtra/Saved/Paks/puffer_temp"
            )
            if (!pufferDir.exists()) pufferDir.mkdirs()

            val targetPak = File(pufferDir, "game_patch_4.5.0.21370.pak")
            FileOutputStream(targetPak).use { output ->
                body.byteStream().copyTo(output)
            }
            return@withContext "Success! Injected into /puffer_temp/game_patch_4.5.0.21370.pak"
        } catch (e: Exception) {
            return@withContext "Error: ${e.message}"
        }
    }
}

@Composable
fun MainAppScreen(onCompileAndDownload: () -> Unit, showToast: (String) -> Unit) {
    val darkBg = Color(0xFF0D0F17)
    val cardBg = Color(0xFF131724)
    val neonBlue = Color(0xFF38BDF8)
    val goldColor = Color(0xFFFFB800)
    var currentTab by remember { mutableStateOf(0) }

    var aimValue by remember { mutableFloatStateOf(9f) }
    var bulletSpread by remember { mutableFloatStateOf(5f) }
    var selectedPreset by remember { mutableStateOf("MEDIUM") }

    Scaffold(
        containerColor = darkBg,
        bottomBar = {
            NavigationBar(containerColor = cardBg) {
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
                .padding(16.dp)
        ) {
            when (currentTab) {
                0 -> {
                    Text("KURONAMI GFX VIP", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = cardBg)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("120 FPS + Custom Server Ready", color = neonBlue, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Knight Core custom patch generator is active.", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                }
                1 -> {
                    Text("FILES & MODS", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = cardBg)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Target: BGMI Mobile [IN]", color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text("AIMBOT + ANTENA V3 NEW", color = neonBlue, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = onCompileAndDownload,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = neonBlue)
                            ) {
                                Text("Download & Auto Inject /puffer_temp", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                2 -> {
                    LazyColumn {
                        item {
                            Text("♦ KNIGHT CORE CUSTOM SERVER", color = goldColor, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Aim Value: ${aimValue.toInt()}", color = Color.White)
                            Slider(value = aimValue, onValueChange = { aimValue = it }, valueRange = 1f..10f, steps = 8)
                            Text("Bullet Spread: ${bulletSpread.toInt()}", color = Color.White)
                            Slider(value = bulletSpread, onValueChange = { bulletSpread = it }, valueRange = 1f..10f, steps = 8)
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                listOf("SMALL", "MEDIUM", "YOUTUBER").forEach { p ->
                                    OutlinedButton(
                                        onClick = { selectedPreset = p },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = if (selectedPreset == p) goldColor else Color.Gray)
                                    ) { Text(p) }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onCompileAndDownload,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = goldColor)
                            ) {
                                Text("COMPILE PAK & INJECT TO GAME", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                3 -> {
                    Text("SETTINGS & PERMISSIONS", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = cardBg)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("• Shizuku: Connected", color = Color(0xFF22C55E))
                            Text("• All File Access: Granted", color = Color(0xFF22C55E))
                            Text("• Target Path: /Paks/puffer_temp", color = neonBlue)
                        }
                    }
                }
            }
        }
    }
}
