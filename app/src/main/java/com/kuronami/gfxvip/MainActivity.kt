package com.kuronami.gfxvip

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

data class ConfigItem(val id: String, val title: String, val desc: String, val img: String, val url: String)

class MainActivity : ComponentActivity() {
    private var mSocket: Socket? = null
    private val client = OkHttpClient()
    private val SERVER_URL = "https://your-server.onrender.com"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }
        initSocket()

        setContent {
            val scope = rememberCoroutineScope()
            var logText by remember { mutableStateOf("Engine Ready. Standby...") }
            var configList by remember { mutableStateOf(listOf<ConfigItem>()) }

            // VPN launcher
            val vpnLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
                if (res.resultCode == Activity.RESULT_OK) {
                    startService(Intent(this, GfxVpnService::class.java))
                    Toast.makeText(this, "VPN & DNS Shield Activated!", Toast.LENGTH_SHORT).show()
                }
            }

            LaunchedEffect(Unit) {
                fetchConfigs { configList = it }
            }

            DisposableEffect(Unit) {
                mSocket?.on("compile_log") { args ->
                    if (args.isNotEmpty()) logText = args[0].toString()
                }
                onDispose { }
            }

            AppNavigation(
                logText = logText,
                configList = configList,
                onToggleVpn = { enable, lowPing ->
                    if (enable) {
                        val vIntent = VpnService.prepare(this)
                        if (vIntent != null) {
                            vpnLauncher.launch(vIntent)
                        } else {
                            val svc = Intent(this, GfxVpnService::class.java).apply {
                                putExtra("LOW_PING", lowPing)
                            }
                            startService(svc)
                        }
                    } else {
                        stopService(Intent(this, GfxVpnService::class.java))
                    }
                },
                onRunGame = { launchGame() },
                onDownloadPak = { url ->
                    scope.launch {
                        val result = downloadAndInject(url)
                        Toast.makeText(this@MainActivity, result, Toast.LENGTH_LONG).show()
                    }
                },
                onSocketBuild = { payload ->
                    mSocket?.emit("start_custom_build", payload)
                }
            )
        }
    }

    private fun initSocket() {
        try {
            mSocket = IO.socket(SERVER_URL)
            mSocket?.connect()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mSocket?.disconnect()
    }

    private fun launchGame() {
        val pkg = "com.pubg.imobile"
        val intent = packageManager.getLaunchIntentForPackage(pkg)
        if (intent != null) {
            startActivity(intent)
        } else {
            Toast.makeText(this, "Target game ($pkg) not installed!", Toast.LENGTH_SHORT).show()
        }
    }

    private suspend fun fetchConfigs(onSuccess: (List<ConfigItem>) -> Unit) = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder().url("$SERVER_URL/api/configs").build()
            val res = client.newCall(req).execute()
            val body = res.body?.string() ?: "[]"
            val arr = JSONArray(body)
            val list = mutableListOf<ConfigItem>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(ConfigItem(
                    id = o.optString("id", "1"),
                    title = o.optString("title", "AIMBOT + ANTENA V3 NEW"),
                    desc = o.optString("description", "Aimbot, Antena, Small Crosshair"),
                    img = o.optString("image_url", "https://picsum.photos/400/200"),
                    url = o.optString("file_url", "$SERVER_URL/api/download-result-pak")
                ))
            }
            withContext(Dispatchers.Main) { onSuccess(list) }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onSuccess(listOf(ConfigItem("1", "AIMBOT + ANTENA V3 NEW", "ALL VERSION - Small Crosshair, 120 FPS", "https://picsum.photos/400/200", "$SERVER_URL/api/download-result-pak")))
            }
        }
    }

    private suspend fun downloadAndInject(url: String): String = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder().url(url).build()
            val res = client.newCall(req).execute()
            if (!res.isSuccessful) return@withContext "Download Error: HTTP ${res.code}"
            val body = res.body ?: return@withContext "Null response body"

            val targetDir = File(
                Environment.getExternalStorageDirectory(),
                "Android/data/com.pubg.imobile/files/UE4Game/ShadowTrackerExtra/ShadowTrackerExtra/Saved/Paks/puffer_temp"
            )
            if (!targetDir.exists()) targetDir.mkdirs()

            val pakFile = File(targetDir, "game_patch_4.5.0.21370.pak")
            FileOutputStream(pakFile).use { out -> body.byteStream().copyTo(out) }
            return@withContext "Applied to /Paks/puffer_temp/game_patch_4.5.0.21370.pak"
        } catch (e: Exception) {
            return@withContext "Error: ${e.message}"
        }
    }
}

@Composable
fun AppNavigation(
    logText: String,
    configList: List<ConfigItem>,
    onToggleVpn: (Boolean, Boolean) -> Unit,
    onRunGame: () -> Unit,
    onDownloadPak: (String) -> Unit,
    onSocketBuild: (JSONObject) -> Unit
) {
    val darkBg = Color(0xFF0D0F17)
    val cardBg = Color(0xFF131724)
    val neonBlue = Color(0xFF38BDF8)
    val gold = Color(0xFFFFB800)

    var tab by remember { mutableStateOf(0) }

    Scaffold(
        containerColor = darkBg,
        bottomBar = {
            NavigationBar(containerColor = cardBg) {
                NavigationBarItem(selected = tab == 0, onClick = { tab = 0 }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Home") })
                NavigationBarItem(selected = tab == 1, onClick = { tab = 1 }, icon = { Icon(Icons.Default.Folder, null) }, label = { Text("Files") })
                NavigationBarItem(selected = tab == 2, onClick = { tab = 2 }, icon = { Icon(Icons.Default.Build, null) }, label = { Text("Server") })
                NavigationBarItem(selected = tab == 3, onClick = { tab = 3 }, icon = { Icon(Icons.Default.Settings, null) }, label = { Text("Settings") })
            }
        }
    ) { p ->
        Box(modifier = Modifier.fillMaxSize().padding(p).padding(16.dp)) {
            when (tab) {
                0 -> HomeScreenView(cardBg, neonBlue, onToggleVpn, onRunGame)
                1 -> FilesScreenView(cardBg, neonBlue, configList, onDownloadPak)
                2 -> CustomServerView(cardBg, gold, logText, onSocketBuild, onDownloadPak)
                3 -> SettingsScreenView(cardBg, neonBlue)
            }
        }
    }
}

@Composable
fun HomeScreenView(cardBg: Color, neonBlue: Color, onToggleVpn: (Boolean, Boolean) -> Unit, onRunGame: () -> Unit) {
    var floating by remember { mutableStateOf(false) }
    var darkMode by remember { mutableStateOf(true) }
    var blocker by remember { mutableStateOf(true) }
    var fps120 by remember { mutableStateOf(true) }
    var lowPing by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(46.dp).clip(CircleShape).background(Color(0xFF1E293B)).border(1.dp, neonBlue, CircleShape), contentAlignment = Alignment.Center) {
                    Text("K", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("KURONAMI GFX.", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    Text("YOUR GAME, YOUR CHOICE !", color = Color.Gray, fontSize = 10.sp)
                }
            }
            Box(modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(Color(0xFF1E1B4B)).padding(horizontal = 10.dp, vertical = 6.dp)) {
                Text("• JOIN US", color = Color(0xFFA855F7), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("QUICK OPTIONS", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        Card(colors = CardDefaults.cardColors(containerColor = cardBg), shape = RoundedCornerShape(14.dp)) {
            Column(modifier = Modifier.padding(12.dp)) {
                RowToggle("Floating Menu", "Quick access overlay in-game", Icons.Default.DesktopWindows, floating) { floating = it }
                RowToggle("Dark Mode", "Force dark theme app-wide", Icons.Default.DarkMode, darkMode) { darkMode = it }
                RowToggle("Update Blocker", "Prevent Play Store auto-update", Icons.Default.Cancel, blocker) {
                    blocker = it
                    onToggleVpn(blocker, lowPing)
                }
                RowToggle("120 FPS Unlock", "Requires supported device", Icons.Default.FlashOn, fps120) { fps120 = it }
                RowToggle("Low Ping Mode", "Force 1.1.1.1 Cloudflare DNS", Icons.Default.Security, lowPing) {
                    lowPing = it
                    if (blocker) onToggleVpn(true, lowPing)
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = onRunGame,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(26.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
        ) {
            Icon(Icons.Default.PlayArrow, null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("RUN--GAME--NOW", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
    }
}

@Composable
fun FilesScreenView(cardBg: Color, neonBlue: Color, list: List<ConfigItem>, onDownloadPak: (String) -> Unit) {
    var search by remember { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxSize()) {
        Text("FILES", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(modifier = Modifier.height(10.dp))
        Surface(color = cardBg, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.School, null, tint = neonBlue)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Target: BGMI Mobile [IN]", color = Color.White, fontSize = 13.sp)
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            placeholder = { Text("Search files, configs...", color = Color.Gray, fontSize = 13.sp) },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
            shape = RoundedCornerShape(10.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(list.filter { it.title.contains(search, ignoreCase = true) }) { item ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), colors = CardDefaults.cardColors(containerColor = cardBg)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        AsyncImage(model = item.img, contentDescription = null, modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(item.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(item.desc, color = Color.Gray, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { onDownloadPak(item.url) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = neonBlue)
                        ) {
                            Text("Download & Inject /puffer_temp", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomServerView(cardBg: Color, gold: Color, logText: String, onSocketBuild: (JSONObject) -> Unit, onDownloadPak: (String) -> Unit) {
    var aim by remember { mutableFloatStateOf(9f) }
    var spread by remember { mutableFloatStateOf(5f) }
    var preset by remember { mutableStateOf("MEDIUM") }

    val guns = remember { mutableStateListOf("M416", "SCAR", "DP28", "AUG", "M249", "UMP9", "MG3", "MK14") }
    val selectedGuns = remember { mutableStateMapOf<String, Boolean>() }
    val vehicles = listOf("BRDM", "BUGGY", "DACIA", "MOTORCYCLE")
    val selectedVehicles = remember { mutableStateMapOf<String, Boolean>() }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Text("♦ KNIGHT CORE CUSTOM SERVER", color = gold, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Spacer(modifier = Modifier.height(10.dp))
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = cardBg)) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("Socket.IO Engine Status:", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text(logText, color = Color(0xFF38BDF8), fontSize = 11.sp)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text("Aim Value: ${aim.toInt()}", color = Color.White, fontSize = 13.sp)
            Slider(value = aim, onValueChange = { aim = it }, valueRange = 1f..10f, steps = 8)
            Text("Bullet Spread: ${spread.toInt()}", color = Color.White, fontSize = 13.sp)
            Slider(value = spread, onValueChange = { spread = it }, valueRange = 1f..10f, steps = 8)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("SMALL", "MEDIUM", "YOUTUBER").forEach { p ->
                    OutlinedButton(
                        onClick = { preset = p },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = if (preset == p) gold else Color.Gray)
                    ) { Text(p, fontSize = 12.sp) }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("NO RECOIL WEAPONS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        items(guns) { gun ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = selectedGuns[gun] ?: false,
                    onCheckedChange = { selectedGuns[gun] = it }
                )
                Text(gun, color = Color.LightGray, fontSize = 13.sp)
            }
        }

        item {
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = {
                    val json = JSONObject().apply {
                        put("aimValue", aim.toInt())
                        put("bulletSpread", spread.toInt())
                        put("preset", preset)
                    }
                    onSocketBuild(json)
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = gold)
            ) {
                Text("START WEBSOCKET UNPACK & REPACK", color = Color.Black, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { onDownloadPak("https://your-server.onrender.com/api/download-result-pak") },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E))
            ) {
                Text("Download Compiled PAK to /puffer_temp", color = Color.White)
            }
        }
    }
}

@Composable
fun SettingsScreenView(cardBg: Color, neonBlue: Color) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    Column(modifier = Modifier.fillMaxSize()) {
        Text("SETTINGS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Card(colors = CardDefaults.cardColors(containerColor = cardBg), shape = RoundedCornerShape(12.dp)) {
            Column(modifier = Modifier.padding(14.dp)) {
                SettingsRow("Setup Shizuku Permission", "Auto-Detected") {
                    Toast.makeText(ctx, "Checking Shizuku Service...", Toast.LENGTH_SHORT).show()
                }
                SettingsRow("Setup All File Access", "Manage Storage") {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        ctx.startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:${ctx.packageName}")))
                    }
                }
                SettingsRow("Disable Battery Optimization", "Bypass Restrict") {
                    val i = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    ctx.startActivity(i)
                }
            }
        }
    }
}

@Composable
fun RowToggle(title: String, desc: String, icon: ImageVector, state: Boolean, onChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Color(0xFF38BDF8), modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(desc, color = Color.Gray, fontSize = 10.sp)
        }
        Switch(checked = state, onCheckedChange = onChange)
    }
}

@Composable
fun SettingsRow(title: String, status: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text("• $status", color = Color(0xFF22C55E), fontSize = 11.sp)
        }
        Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
    }
}
