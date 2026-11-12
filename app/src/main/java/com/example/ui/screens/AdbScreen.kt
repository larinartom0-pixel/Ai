package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.components.CodeBlockView
import com.example.ui.theme.CyberBlack
import com.example.ui.theme.CyberDark
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdbScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val prefs = viewModel.preferencesManager

    var host by remember { mutableStateOf(prefs.adbHost) }
    var port by remember { mutableStateOf(prefs.adbPort.toString()) }
    var pairingPort by remember { mutableStateOf(if (prefs.adbPairingPort > 0) prefs.adbPairingPort.toString() else "") }
    var pairingCode by remember { mutableStateOf(prefs.adbPairingCode) }
    var customAdbCmd by remember { mutableStateOf("") }

    val adbStatusText by viewModel.adbStatusText.collectAsState()
    val isAdbConnected by viewModel.isAdbConnected.collectAsState()
    val isShizukuRunning by viewModel.isShizukuRunning.collectAsState()
    val isShizukuGranted by viewModel.isShizukuGranted.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBlack)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title Bar
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Memory,
                contentDescription = null,
                tint = NeonCyan,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "ADB та Розширене Керування",
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Постійне підключення через Shizuku або Wireless ADB",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        // 🌟 PRIORITY: Shizuku Card for Zero-Hassle Persistent ADB
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isShizukuGranted) Color(0xFF09291D) else Color(0xFF131D2E)
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.5.dp,
                if (isShizukuGranted) NeonEmerald else NeonCyan
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isShizukuGranted) Icons.Default.CheckCircle else Icons.Default.Memory,
                        contentDescription = null,
                        tint = if (isShizukuGranted) NeonEmerald else NeonCyan,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isShizukuGranted) "🟢 Shizuku ADB: АКТИВНО (Постійний доступ)" else "⚡ Постійний ADB через Shizuku",
                        color = if (isShizukuGranted) NeonEmerald else NeonCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isShizukuGranted) {
                        "Додаток має прямі привілеї ADB (UID 2000 shell). Всі запити AI, команди та автоматизація працюють напряму без повторного введення портів чи спарювань!"
                    } else if (isShizukuRunning) {
                        "Сервіс Shizuku запущено на телефоні! Надайте додатку дозвіл, щоб назавжди позбутися необхідності спарювати порти."
                    } else {
                        "Використовуйте Shizuku, щоб надати постійний ADB доступ один раз і не підключати Wireless Debugging вручну кожного разу."
                    },
                    color = Color(0xFFE2E8F0),
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!isShizukuGranted) {
                        Button(
                            onClick = { viewModel.requestShizukuPermission() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonEmerald,
                                contentColor = Color.Black
                            ),
                            modifier = Modifier.weight(1f).testTag("shizuku_grant_button")
                        ) {
                            Text("Надати дозвіл Shizuku", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            val shizukuIntent = context.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
                                ?: context.packageManager.getLaunchIntentForPackage("moe.shizuku.manager")
                            if (shizukuIntent != null) {
                                context.startActivity(shizukuIntent)
                            } else {
                                Toast.makeText(context, "Додаток Shizuku не знайдено на пристрої", Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Відкрити Shizuku", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = { viewModel.checkShizukuStatus() },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Check Shizuku", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // Connection Card (Wireless ADB Fallback)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CyberDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Параметри підключення Wireless ADB",
                    color = NeonCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = host,
                        onValueChange = {
                            host = it
                            prefs.adbHost = it
                        },
                        label = { Text("IP / Хост", fontSize = 11.sp) },
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("adb_host_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = NeonCyan
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedTextField(
                        value = port,
                        onValueChange = {
                            port = it
                            it.toIntOrNull()?.let { p -> prefs.adbPort = p }
                        },
                        label = { Text("Порт (5555)", fontSize = 11.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("adb_port_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = NeonCyan
                        ),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Pairing Section for Android 11+
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = pairingPort,
                        onValueChange = {
                            pairingPort = it
                            it.toIntOrNull()?.let { p -> prefs.adbPairingPort = p }
                        },
                        label = { Text("Порт спарювання", fontSize = 11.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("adb_pairing_port_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = WarningAmber
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedTextField(
                        value = pairingCode,
                        onValueChange = {
                            pairingCode = it
                            prefs.adbPairingCode = it
                        },
                        label = { Text("Код спарювання (6 цифр)", fontSize = 11.sp) },
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("adb_pairing_code_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = WarningAmber
                        ),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.testAdbConnection() },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("test_adb_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Перевірити порт", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            val p = pairingPort.toIntOrNull() ?: 0
                            if (p > 0 && pairingCode.isNotBlank()) {
                                viewModel.pairAdbDevice(p, pairingCode)
                            } else {
                                Toast.makeText(context, "Вкажіть порт і код спарювання", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WarningAmber,
                            contentColor = Color.Black
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pair_adb_button")
                    ) {
                        Text("Спарити пристрій", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ADB Status Output
                CodeBlockView(
                    code = adbStatusText,
                    title = "Статус з'єднання ADB",
                    textColor = if (isAdbConnected) NeonEmerald else Color(0xFFF59E0B)
                )
            }
        }

        // Quick ADB Commands for Extended Control
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CyberDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Швидкі команди розширеного керування",
                    color = NeonEmerald,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val commands = listOf(
                        "devices" to "Підключені девайси",
                        "shell dumpsys battery" to "Стан батареї",
                        "shell input keyevent 3" to "Кнопка Додому (Home)",
                        "shell input keyevent 4" to "Кнопка Назад (Back)",
                        "shell pm list packages -3" to "Сторонні додатки",
                        "shell screencap -p /sdcard/screen.png" to "Скріншот",
                        "shell logcat -d -t 30" to "Останні логи"
                    )

                    commands.forEach { (cmd, label) ->
                        OutlinedButton(
                            onClick = { viewModel.runAdbCommand(cmd) },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = TextPrimary
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
                        ) {
                            Text(label, fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = customAdbCmd,
                        onValueChange = { customAdbCmd = it },
                        placeholder = { Text("Власна ADB команда (напр. shell input tap 500 500)", fontSize = 11.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("custom_adb_command_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = NeonCyan
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (customAdbCmd.isNotBlank()) {
                                viewModel.runAdbCommand(customAdbCmd)
                                customAdbCmd = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = CyberBlack)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Run")
                    }
                }
            }
        }

        // Termux Bridge Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Smartphone, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Інтеграція з Termux (Python + ADB)",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Для повного оточення Python з бібліотеками (requests, numpy, beautifulsoup) та утилітою adb виконайте у додатку Termux наступну команду:",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                val termuxCmd = "pkg update && pkg install python android-tools -y"
                CodeBlockView(
                    code = termuxCmd,
                    title = "Команда встановлення в Termux"
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = {
                            val launchIntent = context.packageManager.getLaunchIntentForPackage("com.termux")
                            if (launchIntent != null) {
                                context.startActivity(launchIntent)
                            } else {
                                Toast.makeText(context, "Termux не знайдено на пристрої", Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1E293B),
                            contentColor = NeonCyan
                        )
                    ) {
                        Icon(Icons.Default.Launch, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Відкрити Termux", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
