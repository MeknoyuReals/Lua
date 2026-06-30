package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ScriptItem
import com.example.ui.MeknoyuViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel: MeknoyuViewModel by viewModels {
                MeknoyuViewModel.Factory(application)
            }
            val context = LocalContext.current

            // React to status messages (Toasts)
            LaunchedEffect(key1 = true) {
                viewModel.statusMessage.collectLatest { message ->
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                }
            }

            MeknoyuTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        MeknoyuBottomNavigation(viewModel)
                    },
                    containerColor = CyberBlack
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        // Cyber background glow lines
                        CyberBackgroundDecoration()

                        val currentTab by viewModel.currentTab.collectAsState()
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp)
                        ) {
                            MeknoyuHeader()
                            
                            Spacer(modifier = Modifier.height(8.dp))

                            when (currentTab) {
                                0 -> ObfuscatorScreen(viewModel)
                                1 -> DeobfuscatorScreen(viewModel)
                                2 -> HistoryScreen(viewModel)
                                3 -> ApiSetupScreen()
                            }
                        }

                        // Loading Overlay
                        val isLoading by viewModel.isLoading.collectAsState()
                        if (isLoading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.7f))
                                    .clickable(enabled = false) {},
                                contentAlignment = Alignment.Center
                            ) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                                    border = BorderStroke(1.dp, CyberCyan),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        CircularProgressIndicator(color = CyberCyan)
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = "Memproses Script Lua...",
                                            color = CyberWhiteText,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- Brand Header ---

@Composable
fun MeknoyuHeader() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .border(
                BorderStroke(1.dp, CyberMutedPurple),
                shape = RoundedCornerShape(12.dp)
            )
            .background(CyberDarkCard, shape = RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "MEKNOYU LUA",
                color = CyberCyan,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp
            )
            Text(
                text = "Secure Roblox Luau Engine",
                color = CyberGrayText,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        // Pulse Online Status Indicator
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(CyberToxicGreen.copy(alpha = alpha))
            )
            Text(
                text = "SECURE",
                color = CyberToxicGreen,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
        }
    }
}

// --- Background Cyber Line Decoration ---

@Composable
fun CyberBackgroundDecoration() {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(CyberDarkSurface.copy(alpha = 0.15f), Color.Transparent),
                        radius = 800f
                    )
                )
        )
    }
}

// --- Custom Modern M3 Bottom Navigation Bar ---

@Composable
fun MeknoyuBottomNavigation(viewModel: MeknoyuViewModel) {
    val currentTab by viewModel.currentTab.collectAsState()

    NavigationBar(
        containerColor = CyberDarkCard,
        tonalElevation = 8.dp,
        modifier = Modifier
            .border(BorderStroke(1.dp, CyberMutedPurple))
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        NavigationBarItem(
            selected = currentTab == 0,
            onClick = { viewModel.setTab(0) },
            icon = { Icon(Icons.Filled.Lock, contentDescription = "Obfuscator") },
            label = { Text("Obfuscator", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = CyberBlack,
                selectedTextColor = CyberCyan,
                indicatorColor = CyberCyan,
                unselectedIconColor = CyberGrayText,
                unselectedTextColor = CyberGrayText
            ),
            modifier = Modifier.testTag("nav_obfuscator")
        )
        NavigationBarItem(
            selected = currentTab == 1,
            onClick = { viewModel.setTab(1) },
            icon = { Icon(Icons.Filled.Edit, contentDescription = "Deobfuscator") },
            label = { Text("Deobfuscator", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = CyberBlack,
                selectedTextColor = CyberCyan,
                indicatorColor = CyberCyan,
                unselectedIconColor = CyberGrayText,
                unselectedTextColor = CyberGrayText
            ),
            modifier = Modifier.testTag("nav_deobfuscator")
        )
        NavigationBarItem(
            selected = currentTab == 2,
            onClick = { viewModel.setTab(2) },
            icon = { Icon(Icons.Filled.Refresh, contentDescription = "Riwayat") },
            label = { Text("Riwayat", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = CyberBlack,
                selectedTextColor = CyberCyan,
                indicatorColor = CyberCyan,
                unselectedIconColor = CyberGrayText,
                unselectedTextColor = CyberGrayText
            ),
            modifier = Modifier.testTag("nav_history")
        )
        NavigationBarItem(
            selected = currentTab == 3,
            onClick = { viewModel.setTab(3) },
            icon = { Icon(Icons.Filled.Settings, contentDescription = "API Status") },
            label = { Text("API Setup", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = CyberBlack,
                selectedTextColor = CyberCyan,
                indicatorColor = CyberCyan,
                unselectedIconColor = CyberGrayText,
                unselectedTextColor = CyberGrayText
            ),
            modifier = Modifier.testTag("nav_api")
        )
    }
}

// ==========================================
// SCREEN 1: LUA OBFUSCATOR
// ==========================================

@Composable
fun ObfuscatorScreen(viewModel: MeknoyuViewModel) {
    val context = LocalContext.current
    val inputCode by viewModel.obfuscatorInput.collectAsState()
    val outputCode by viewModel.obfuscatorOutput.collectAsState()
    val clipboardManager = LocalClipboardManager.current

    // Configuration states
    val removeComments by viewModel.optRemoveComments.collectAsState()
    val renameVariables by viewModel.optRenameVariables.collectAsState()
    val encryptStrings by viewModel.optEncryptStrings.collectAsState()
    val injectJunk by viewModel.optInjectJunk.collectAsState()
    val minify by viewModel.optMinify.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Monospace input container
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberDarkCard),
                border = BorderStroke(1.dp, CyberMutedPurple),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SKRIP LUA ASLI (ROBLOX)",
                            color = CyberWhiteText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Row {
                            TextButton(
                                onClick = {
                                    viewModel.obfuscatorInput.value = """
-- Roblox Demo Script Meknoyu
local message = "Halo Robloxian!"
local players = game:GetService("Players")
local localPlayer = players.LocalPlayer

local function greetUser(player)
    local greetStr = message .. " " .. player.Name
    print(greetStr)
    return greetStr
end

greetUser(localPlayer)
                                    """.trimIndent()
                                }
                            ) {
                                Text("Load Demo", color = CyberCyan, fontSize = 11.sp)
                            }
                            IconButton(onClick = { viewModel.obfuscatorInput.value = "" }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Bersihkan", tint = CyberMagenta)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = inputCode,
                        onValueChange = { viewModel.obfuscatorInput.value = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .testTag("obfuscator_input_field"),
                        placeholder = {
                            Text(
                                "Paste script Roblox Lua anda di sini...",
                                color = CyberGrayText,
                                fontSize = 12.sp
                            )
                        },
                        textStyle = LocalTextStyle.current.copy(
                            color = CyberWhiteText,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = CyberMutedPurple,
                            focusedContainerColor = CyberBlack,
                            unfocusedContainerColor = CyberBlack
                        )
                    )
                }
            }
        }

        // Configuration Panel
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberDarkCard),
                border = BorderStroke(1.dp, CyberMutedPurple),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "PENGATURAN SENSOR / OBFUSKASI",
                        color = CyberCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Hapus Komentar", color = CyberWhiteText, fontSize = 13.sp)
                            Text("Membersihkan tag '--'", color = CyberGrayText, fontSize = 10.sp)
                        }
                        Switch(
                            checked = removeComments,
                            onCheckedChange = { viewModel.optRemoveComments.value = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = CyberCyan)
                        )
                    }

                    Divider(color = CyberMutedPurple.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Acak Variabel Lokal", color = CyberWhiteText, fontSize = 13.sp)
                            Text("Ubah locals ke _mek_0x8f", color = CyberGrayText, fontSize = 10.sp)
                        }
                        Switch(
                            checked = renameVariables,
                            onCheckedChange = { viewModel.optRenameVariables.value = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = CyberCyan)
                        )
                    }

                    Divider(color = CyberMutedPurple.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Enkripsi String Literal", color = CyberWhiteText, fontSize = 13.sp)
                            Text("Sembunyikan string ke byte table", color = CyberGrayText, fontSize = 10.sp)
                        }
                        Switch(
                            checked = encryptStrings,
                            onCheckedChange = { viewModel.optEncryptStrings.value = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = CyberCyan)
                        )
                    }

                    Divider(color = CyberMutedPurple.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Injeksi Kode Sampah", color = CyberWhiteText, fontSize = 13.sp)
                            Text("Menambah baris palsu yang aman", color = CyberGrayText, fontSize = 10.sp)
                        }
                        Switch(
                            checked = injectJunk,
                            onCheckedChange = { viewModel.optInjectJunk.value = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = CyberCyan)
                        )
                    }

                    Divider(color = CyberMutedPurple.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Minify (Satu Baris)", color = CyberWhiteText, fontSize = 13.sp)
                            Text("Kompresi ukuran berkas maksimal", color = CyberGrayText, fontSize = 10.sp)
                        }
                        Switch(
                            checked = minify,
                            onCheckedChange = { viewModel.optMinify.value = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = CyberCyan)
                        )
                    }
                }
            }
        }

        // Action Trigger Button
        item {
            Button(
                onClick = { viewModel.performObfuscation() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("obfuscate_trigger_button"),
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Lock, contentDescription = "Mulai Sensor", tint = CyberBlack)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    "SENSORKAN SCRIPT (OBFUSCATE)",
                    color = CyberBlack,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp
                )
            }
        }

        // Output Panel
        if (outputCode.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CyberDarkCard),
                    border = BorderStroke(1.dp, CyberCyan),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "OUTPUT LUA TER-OBFUSKASI",
                                color = CyberCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(outputCode))
                                    Toast.makeText(context, "Kode berhasil disalin!", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(Icons.Filled.Share, contentDescription = "Salin", tint = CyberCyan)
                            }
                        }

                        OutlinedTextField(
                            value = outputCode,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .testTag("obfuscator_output_field"),
                            textStyle = LocalTextStyle.current.copy(
                                color = CyberCyan,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberCyan,
                                unfocusedBorderColor = CyberMutedPurple,
                                focusedContainerColor = CyberBlack,
                                unfocusedContainerColor = CyberBlack
                            )
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// SCREEN 2: LUA DEOBFUSCATOR & BEAUTIFIER
// ==========================================

@Composable
fun DeobfuscatorScreen(viewModel: MeknoyuViewModel) {
    val downloadUrl by viewModel.downloadUrlInput.collectAsState()
    val inputCode by viewModel.deobfuscatorInput.collectAsState()
    val outputCode by viewModel.deobfuscatorOutput.collectAsState()
    val aiExplanation by viewModel.deobfuscatorExplanation.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // URL Downloader Input Panel
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberDarkCard),
                border = BorderStroke(1.dp, CyberMutedPurple),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "UNDUH SCRIPT LUA DARI WEBSITES",
                        color = CyberWhiteText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = downloadUrl,
                            onValueChange = { viewModel.downloadUrlInput.value = it },
                            placeholder = { Text("Tautan URL (Pastebin/Raw GitHub)", color = CyberGrayText, fontSize = 11.sp) },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("downloader_url_field"),
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(color = CyberWhiteText, fontSize = 12.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberCyan,
                                unfocusedBorderColor = CyberMutedPurple,
                                focusedContainerColor = CyberBlack,
                                unfocusedContainerColor = CyberBlack
                            )
                        )

                        Button(
                            onClick = { viewModel.downloadScriptFromUrl() },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberTeal),
                            modifier = Modifier
                                .height(50.dp)
                                .testTag("download_trigger_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Unduh", tint = CyberBlack)
                        }
                    }
                }
            }
        }

        // Code Editor Input for Deobfuscating
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberDarkCard),
                border = BorderStroke(1.dp, CyberMutedPurple),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "KODE TER-OBFUSKASI / SENSOR",
                            color = CyberWhiteText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Row {
                            TextButton(
                                onClick = {
                                    viewModel.deobfuscatorInput.value = """
-- Roblox Demo Obfuscated Script
(function()local b={108,111,99,97,108,32,112,108,97,121,101,114,32,61,32,103,97,109,101,46,80,108,97,121,101,114,115,46,76,111,99,97,108,80,108,97,121,101,114,10,112,114,105,110,116,40,34,77,101,107,110,111,121,117,32,68,101,111,98,102,117,115,99,97,116,101,34,41}local s=""for i=1,#b do s=s..string.char(b[i])end return loadstring(s)()end)()
                                    """.trimIndent()
                                }
                            ) {
                                Text("Demo Obf", color = CyberCyan, fontSize = 11.sp)
                            }
                            IconButton(onClick = { viewModel.deobfuscatorInput.value = "" }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Bersihkan", tint = CyberMagenta)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = inputCode,
                        onValueChange = { viewModel.deobfuscatorInput.value = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .testTag("deobfuscator_input_field"),
                        placeholder = {
                            Text(
                                "Tempel skrip tersensor/obfuscated atau unduh dari URL...",
                                color = CyberGrayText,
                                fontSize = 12.sp
                            )
                        },
                        textStyle = LocalTextStyle.current.copy(
                            color = CyberWhiteText,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = CyberMutedPurple,
                            focusedContainerColor = CyberBlack,
                            unfocusedContainerColor = CyberBlack
                        )
                    )
                }
            }
        }

        // Actions: Pola Lokal & AI Integration
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Pola Lokal
                Button(
                    onClick = { viewModel.performLocalDeobfuscation() },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberDarkSurface),
                    border = BorderStroke(1.dp, CyberTeal),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("local_deobf_button")
                ) {
                    Icon(Icons.Filled.Build, contentDescription = "Offline Pola", tint = CyberTeal)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("DEKODE POLA", color = CyberTeal, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }

                // AI Deobfuscator
                Button(
                    onClick = { viewModel.performAIDeobfuscation() },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1.2f)
                        .height(50.dp)
                        .testTag("ai_deobf_button")
                ) {
                    Icon(Icons.Filled.Star, contentDescription = "AI Gemini", tint = CyberBlack)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("DEOBFUSCATE AI", color = CyberBlack, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }
        }

        // Decoded Output Panel
        if (outputCode.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CyberDarkCard),
                    border = BorderStroke(1.dp, CyberCyan),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "KODE HASIL DEOBFUSKASI",
                                color = CyberCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(outputCode))
                                    Toast.makeText(context, "Kode asli berhasil disalin!", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(Icons.Filled.Share, contentDescription = "Salin", tint = CyberCyan)
                            }
                        }

                        OutlinedTextField(
                            value = outputCode,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .testTag("deobfuscator_output_field"),
                            textStyle = LocalTextStyle.current.copy(
                                color = CyberToxicGreen,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberCyan,
                                unfocusedBorderColor = CyberMutedPurple,
                                focusedContainerColor = CyberBlack,
                                unfocusedContainerColor = CyberBlack
                            )
                        )
                    }
                }
            }
        }

        // AI Analysis & Safety Explanation Details
        if (aiExplanation.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                    border = BorderStroke(1.dp, CyberMagenta),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Icon(Icons.Filled.Info, contentDescription = "Analisis AI", tint = CyberMagenta)
                            Text(
                                text = "ANALISIS KEAMANAN & FITUR SKRIP (AI)",
                                color = CyberMagenta,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Text(
                            text = aiExplanation,
                            color = CyberWhiteText,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// SCREEN 3: SAVED PROJECTS / RIWAYAT
// ==========================================

@Composable
fun HistoryScreen(viewModel: MeknoyuViewModel) {
    val history by viewModel.historyState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "DATABASE PROYEK LOKAL",
                color = CyberWhiteText,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
            if (history.isNotEmpty()) {
                TextButton(
                    onClick = { viewModel.clearAllHistory() },
                    colors = ButtonDefaults.textButtonColors(contentColor = CyberMagenta)
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = "Hapus Semua")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Hapus Semua", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (history.isEmpty()) {
            // Friendly Empty State
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Filled.Info,
                        contentDescription = "Database kosong",
                        tint = CyberMutedPurple,
                        modifier = Modifier.size(60.dp)
                    )
                    Text(
                        text = "Belum Ada Riwayat Skrip",
                        color = CyberGrayText,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Gunakan fitur Obfuscator atau Deobfuscator\nuntuk menyimpan proyek anda secara aman.",
                        color = CyberGrayText.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(history) { item ->
                    HistoryCard(item, viewModel)
                }
            }
        }
    }
}

@Composable
fun HistoryCard(item: ScriptItem, viewModel: MeknoyuViewModel) {
    val isObf = item.type == "OBFUSCATE"
    val typeColor = if (isObf) CyberCyan else CyberTeal
    val formatter = remember { SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()) }
    val dateStr = formatter.format(Date(item.timestamp))

    Card(
        colors = CardDefaults.cardColors(containerColor = CyberDarkCard),
        border = BorderStroke(1.dp, CyberMutedPurple),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        color = CyberWhiteText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1
                    )
                    Text(
                        text = dateStr,
                        color = CyberGrayText,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Type Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(typeColor.copy(alpha = 0.15f))
                        .border(BorderStroke(0.5.dp, typeColor), shape = RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = item.type,
                        color = typeColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { viewModel.loadHistoryItem(item) },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberDarkSurface),
                    border = BorderStroke(0.5.dp, CyberCyan),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Filled.Info, contentDescription = "Load", tint = CyberCyan, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Buka Proyek", color = CyberCyan, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }

                IconButton(
                    onClick = { viewModel.deleteHistoryItem(item.id) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = "Hapus", tint = CyberMagenta, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

// ==========================================
// SCREEN 4: API STATUS & CONFIGURATION GUIDE
// ==========================================

@Composable
fun ApiSetupScreen() {
    val apiKeyVal = com.example.BuildConfig.GEMINI_API_KEY
    val isApiKeyConfigured = apiKeyVal.isNotEmpty() && apiKeyVal != "MY_GEMINI_API_KEY"

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "PENGATURAN INTEGRASI API KEAMANAN",
                color = CyberWhiteText,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        // Gemini API Status Box
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberDarkCard),
                border = BorderStroke(1.dp, if (isApiKeyConfigured) CyberToxicGreen else CyberSoftWarning),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "STATUS API GEMINI AI",
                        color = CyberGrayText,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(RoundedCornerShape(50))
                                .background(if (isApiKeyConfigured) CyberToxicGreen else CyberSoftWarning)
                        )
                        Text(
                            text = if (isApiKeyConfigured) "TERHUBUNG (Online)" else "TIDAK AKTIF (Butuh Konfigurasi)",
                            color = if (isApiKeyConfigured) CyberToxicGreen else CyberSoftWarning,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (isApiKeyConfigured) {
                            "API Key berhasil diidentifikasi secara aman dari runtime Secrets panel. Anda sekarang dapat menggunakan model canggih gemini-3.5-flash untuk mendekompresi dan menganalisis Roblox Luau script secara cerdas."
                        } else {
                            "API Key Gemini belum diatur. Fitur 'Deobfuscate AI' memerlukan API Key agar dapat memproses skrip Roblox Lua secara otomatis menggunakan AI."
                        },
                        color = CyberWhiteText,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Quick Tutorial Guide
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberDarkCard),
                border = BorderStroke(1.dp, CyberMutedPurple),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.Lock, contentDescription = "Aman", tint = CyberCyan)
                        Text(
                            text = "CARA MENGATUR API KEY SECARA AMAN",
                            color = CyberCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    val steps = listOf(
                        "1. Buka halaman utama Google AI Studio.",
                        "2. Cari panel 'Secrets' di sisi menu panel platform.",
                        "3. Masukkan variable key dengan nama: GEMINI_API_KEY.",
                        "4. Tempelkan API Key anda, lalu tekan simpan.",
                        "5. Kompiler platform kami akan otomatis menyisipkan key tersebut ke dalam berkas .env secara rahasia untuk melindungi kredensial anda dari dekompilasi."
                    )

                    steps.forEach { step ->
                        Text(
                            text = step,
                            color = CyberWhiteText,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(vertical = 4.dp),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Security Warning Constraint Box
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                border = BorderStroke(1.dp, CyberSoftWarning),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Outlined.Warning, contentDescription = "Peringatan", tint = CyberSoftWarning)
                        Text(
                            text = "SECURITY WARNING",
                            color = CyberSoftWarning,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "I have included your API keys in the generated APK file for this prototype. Please be aware that Android APKs can be easily decompiled, and these keys can be extracted by anyone who has access to the file. Do not share this APK file publicly or with unauthorized individuals to prevent potential misuse.",
                        color = CyberGrayText,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
