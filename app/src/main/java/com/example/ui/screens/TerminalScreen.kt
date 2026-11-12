package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.theme.CyberBlack
import com.example.ui.theme.CyberDark
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.TerminalBg
import com.example.ui.theme.TerminalGreen
import com.example.ui.theme.TerminalText
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TerminalScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val terminalHistory by viewModel.manualTerminalHistory.collectAsState()
    var commandInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(terminalHistory.size) {
        if (terminalHistory.isNotEmpty()) {
            listState.animateScrollToItem(terminalHistory.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TerminalBg)
            .imePadding()
    ) {
        // Terminal Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CyberDark)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Terminal,
                contentDescription = null,
                tint = NeonEmerald,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Linux Shell / Termux Console",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "Workspace: ~/workspace",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.weight(1f))

            IconButton(
                onClick = {
                    val allText = terminalHistory.joinToString("\n")
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Terminal Log", allText))
                    Toast.makeText(context, "Логи терміналу скопійовано", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.size(32.dp).testTag("copy_terminal_button")
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = NeonCyan, modifier = Modifier.size(18.dp))
            }

            Spacer(modifier = Modifier.width(6.dp))

            IconButton(
                onClick = { viewModel.clearManualTerminal() },
                modifier = Modifier.size(32.dp).testTag("clear_terminal_button")
            ) {
                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
            }
        }

        // Quick Command Shortcuts
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F172A))
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val quickCmds = listOf(
                "pwd",
                "ls -la",
                "uname -a",
                "df -h",
                "python3 --version",
                "which python3",
                "pm list packages | head -n 15",
                "cat /proc/meminfo | head -n 5"
            )

            quickCmds.forEach { cmd ->
                SuggestionChip(
                    onClick = {
                        commandInput = cmd
                        viewModel.runManualTerminalCommand(cmd)
                    },
                    label = {
                        Text(
                            text = cmd,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = NeonEmerald
                        )
                    },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = CyberDark
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
                )
            }
        }

        // Terminal Output Console
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(terminalHistory) { line ->
                val color = when {
                    line.startsWith("$ ") -> NeonCyan
                    line.startsWith("Stderr:") -> Color(0xFFEF4444)
                    line.contains("completed with exit code 0") -> NeonEmerald
                    line.contains("exit code") -> Color(0xFFF59E0B)
                    else -> TerminalText
                }
                Text(
                    text = line,
                    color = color,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        }

        // Bottom Input Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CyberDark)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$",
                color = NeonEmerald,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.padding(end = 8.dp)
            )

            OutlinedTextField(
                value = commandInput,
                onValueChange = { commandInput = it },
                placeholder = { Text("Введіть команду...", color = TextSecondary, fontSize = 13.sp) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("terminal_command_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = CyberBlack,
                    unfocusedContainerColor = CyberBlack,
                    focusedBorderColor = NeonEmerald,
                    unfocusedBorderColor = Color(0xFF334155)
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    if (commandInput.isNotBlank()) {
                        viewModel.runManualTerminalCommand(commandInput)
                        commandInput = ""
                    }
                },
                enabled = commandInput.isNotBlank(),
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (commandInput.isNotBlank()) NeonEmerald else Color(0xFF334155))
                    .testTag("run_terminal_command_button")
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Run",
                    tint = if (commandInput.isNotBlank()) CyberBlack else TextSecondary
                )
            }
        }
    }
}
