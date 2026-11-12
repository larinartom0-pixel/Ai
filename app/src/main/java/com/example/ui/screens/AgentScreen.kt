package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ChatMessageEntity
import com.example.data.model.AgentStatus
import com.example.ui.AppTab
import com.example.ui.MainViewModel
import com.example.ui.components.CodeBlockView
import com.example.ui.components.CountdownBanner
import com.example.ui.theme.CyberBlack
import com.example.ui.theme.CyberDark
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonCyanLight
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AgentScreen(viewModel: MainViewModel) {
    val selectedSessionId by viewModel.selectedSessionId.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val messages by viewModel.currentMessages.collectAsState()
    val agentStatus by viewModel.agentStatus.collectAsState()
    val apiKey = viewModel.preferencesManager.geminiApiKey

    var promptInput by remember { mutableStateOf("") }
    var showSessionMenu by remember { mutableStateOf(false) }
    var showNewSessionDialog by remember { mutableStateOf(false) }
    var newSessionTitle by remember { mutableStateOf("") }

    val listState = rememberLazyListState()

    // Auto scroll when messages change
    LaunchedEffect(messages.size, agentStatus) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val currentSession = sessions.find { it.id == selectedSessionId }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBlack)
            .imePadding()
    ) {
        // Top Session Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CyberDark)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showSessionMenu = true }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = "Session",
                        tint = NeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = currentSession?.title ?: "Select or create task",
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Dropdown",
                        tint = TextSecondary
                    )
                }

                DropdownMenu(
                    expanded = showSessionMenu,
                    onDismissRequest = { showSessionMenu = false },
                    modifier = Modifier.background(CyberSurface)
                ) {
                    sessions.forEach { sess ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = sess.title,
                                    color = if (sess.id == selectedSessionId) NeonCyan else TextPrimary,
                                    maxLines = 1
                                )
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        viewModel.deleteSession(sess.id)
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            },
                            onClick = {
                                viewModel.selectSession(sess.id)
                                showSessionMenu = false
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = {
                            Text("+ Нова задача", color = NeonCyanLight, fontWeight = FontWeight.Bold)
                        },
                        onClick = {
                            showSessionMenu = false
                            showNewSessionDialog = true
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            IconButton(
                onClick = { showNewSessionDialog = true },
                modifier = Modifier.testTag("new_session_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Session", tint = NeonCyan)
            }
        }

        // Missing API Key Banner
        if (apiKey.isBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF3B2508))
                    .clickable { viewModel.setTab(AppTab.SETTINGS) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Key, contentDescription = "Key", tint = WarningAmber, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "API ключ Gemini не вказано. Натисніть тут для введення.",
                    color = WarningAmber,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Active WaitingRetry / RateLimit Countdown Banner
        if (agentStatus is AgentStatus.WaitingRetry) {
            val retryState = agentStatus as AgentStatus.WaitingRetry
            Box(modifier = Modifier.padding(12.dp)) {
                CountdownBanner(
                    countdownSeconds = retryState.countdownSeconds,
                    totalSeconds = viewModel.preferencesManager.retryIntervalSeconds,
                    attempt = retryState.attempt,
                    reason = retryState.reason,
                    onRetryNow = { viewModel.retryRateLimitNow() },
                    onCancel = { viewModel.stopCurrentTask() }
                )
            }
        }

        // Messages & Execution History
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { Spacer(modifier = Modifier.height(6.dp)) }

            if (messages.isEmpty()) {
                item {
                    EmptyAgentPlaceholder(onSuggestionClick = { promptInput = it })
                }
            } else {
                items(messages, key = { it.id }) { message ->
                    ChatMessageItem(message = message)
                }
            }

            // Running Agent Indicator
            if (agentStatus is AgentStatus.Running) {
                val runningState = agentStatus as AgentStatus.Running
                item {
                    AgentRunningCard(step = runningState.step, action = runningState.action) {
                        viewModel.stopCurrentTask()
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }
        }

        // Bottom Quick Suggestions
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SuggestionChip(
                onClick = { promptInput = "Напиши та запусти Python скрипт перевірки системи" },
                label = { Text("🐍 Python скрипт", fontSize = 11.sp) },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = CyberDark,
                    labelColor = NeonCyan
                ),
                border = SuggestionChipDefaults.suggestionChipBorder(
                    enabled = true,
                    borderColor = Color(0xFF334155)
                )
            )
            SuggestionChip(
                onClick = { promptInput = "Перевір доступні бінарники в терміналі (python, sh, adb)" },
                label = { Text("⚡ Перевірити бінарники", fontSize = 11.sp) },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = CyberDark,
                    labelColor = NeonEmerald
                ),
                border = SuggestionChipDefaults.suggestionChipBorder(
                    enabled = true,
                    borderColor = Color(0xFF334155)
                )
            )
            SuggestionChip(
                onClick = { promptInput = "Виконай команду через ADB: покажи підключені пристрої" },
                label = { Text("📱 ADB команди", fontSize = 11.sp) },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = CyberDark,
                    labelColor = WarningAmber
                ),
                border = SuggestionChipDefaults.suggestionChipBorder(
                    enabled = true,
                    borderColor = Color(0xFF334155)
                )
            )
        }

        // Bottom Input Field
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CyberDark)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = promptInput,
                onValueChange = { promptInput = it },
                placeholder = { Text("Опишіть задачу (код, python, термінал, adb)...", color = TextSecondary, fontSize = 13.sp) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("task_input_field"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = CyberBlack,
                    unfocusedContainerColor = CyberBlack,
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = Color(0xFF334155)
                ),
                shape = RoundedCornerShape(20.dp),
                maxLines = 4
            )

            Spacer(modifier = Modifier.width(8.dp))

            val isRunning = agentStatus is AgentStatus.Running || agentStatus is AgentStatus.WaitingRetry

            if (isRunning) {
                IconButton(
                    onClick = { viewModel.stopCurrentTask() },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444))
                        .testTag("stop_task_button")
                ) {
                    Icon(Icons.Default.Stop, contentDescription = "Stop", tint = Color.White)
                }
            } else {
                IconButton(
                    onClick = {
                        if (promptInput.isNotBlank()) {
                            viewModel.submitUserTask(promptInput)
                            promptInput = ""
                        }
                    },
                    enabled = promptInput.isNotBlank(),
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (promptInput.isNotBlank()) NeonCyan else Color(0xFF334155))
                        .testTag("send_task_button")
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "Send",
                        tint = if (promptInput.isNotBlank()) CyberBlack else TextSecondary
                    )
                }
            }
        }
    }

    // New Session Dialog
    if (showNewSessionDialog) {
        AlertDialog(
            onDismissRequest = { showNewSessionDialog = false },
            title = { Text("Нова задача", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = newSessionTitle,
                    onValueChange = { newSessionTitle = it },
                    placeholder = { Text("Наприклад: Python скрипт аналізу логів") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val title = if (newSessionTitle.isNotBlank()) newSessionTitle else "Задача #${sessions.size + 1}"
                        viewModel.createNewSession(title)
                        newSessionTitle = ""
                        showNewSessionDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = CyberBlack)
                ) {
                    Text("Створити")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewSessionDialog = false }) {
                    Text("Скасувати", color = TextSecondary)
                }
            },
            containerColor = CyberSurface
        )
    }
}

@Composable
fun EmptyAgentPlaceholder(onSuggestionClick: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.SmartToy,
            contentDescription = null,
            tint = NeonCyanLight,
            modifier = Modifier.size(60.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "AI Автономний Агент",
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Введіть задачу для виконання. Агент напише код, запустить Python скрипт, виконає команди в терміналі або використає ADB з автоматичним контролем контексту та авто-повтором.",
            color = TextSecondary,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun ChatMessageItem(message: ChatMessageEntity) {
    when (message.role) {
        "user" -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .clip(RoundedCornerShape(16.dp, 16.dp, 2.dp, 16.dp))
                        .background(Color(0xFF1E293B))
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp, 16.dp, 2.dp, 16.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = message.content,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        "tool" -> {
            // Card for tool actions (shell command, python, adb)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CyberDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Header with tool badge and exit code
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val (icon, badgeColor) = when (message.toolName) {
                            "run_python" -> Pair(Icons.Default.Code, NeonCyan)
                            "adb_command" -> Pair(Icons.Default.Memory, WarningAmber)
                            else -> Pair(Icons.Default.Terminal, NeonEmerald)
                        }

                        Icon(icon, contentDescription = null, tint = badgeColor, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = message.toolName ?: "action",
                            color = badgeColor,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        if (message.exitCode != null) {
                            val codeColor = if (message.exitCode == 0) NeonEmerald else Color(0xFFEF4444)
                            Text(
                                text = "exit ${message.exitCode}",
                                color = codeColor,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(codeColor.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        if (message.executionTimeMs != null && message.executionTimeMs > 0) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${message.executionTimeMs}ms",
                                color = TextSecondary,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    if (!message.thought.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "🧠 ${message.thought}",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }

                    if (!message.toolResult.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        CodeBlockView(
                            code = message.toolResult,
                            title = "Результат виконання"
                        )
                    }
                }
            }
        }

        else -> {
            // Model response
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CyberDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0F2634)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.SmartToy,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "AIAgent",
                            color = NeonCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = message.content,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

@Composable
fun AgentRunningCard(step: Int, action: String, onCancel: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0C1929)),
        border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = NeonCyan,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Крок #$step",
                    color = NeonCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
                Text(
                    text = action,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    maxLines = 1
                )
            }
            IconButton(onClick = onCancel, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Stop, contentDescription = "Stop", tint = Color(0xFFEF4444))
            }
        }
    }
}
