package com.example.domain.executor

import android.content.Context
import com.example.data.local.PreferencesManager
import com.example.data.model.ToolExecutionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket

class AdbExecutor(
    private val context: Context,
    private val preferencesManager: PreferencesManager,
    private val shellExecutor: ShellExecutor
) {
    suspend fun executeCommand(adbCommand: String): ToolExecutionResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val host = preferencesManager.adbHost
        val port = preferencesManager.adbPort

        // Check if `adb` binary is available
        val adbCheck = shellExecutor.execute("which adb || [ -f /data/data/com.termux/files/usr/bin/adb ] && echo has_adb")
        val hasAdbBinary = adbCheck.stdout.contains("adb") || adbCheck.stdout.contains("has_adb")

        if (hasAdbBinary) {
            val fullCmd = if (host.isNotBlank() && port > 0) {
                // If adb is configured for remote/wireless
                "adb connect $host:$port >/dev/null 2>&1; adb -s $host:$port $adbCommand"
            } else {
                "adb $adbCommand"
            }
            return@withContext shellExecutor.execute(fullCmd)
        }

        // If no adb binary, test TCP socket connection to Wireless Debugging / adbd daemon
        val socketTestResult = testSocketConnection(host, port)
        if (!socketTestResult.first) {
            val duration = System.currentTimeMillis() - startTime
            return@withContext ToolExecutionResult(
                toolName = "adb_command",
                stdout = "",
                stderr = "ADB target unreachable at $host:$port (${socketTestResult.second}).\nMake sure 'Wireless Debugging' is enabled in Developer Options.\nYou can also install adb in Termux via: pkg install android-tools",
                exitCode = 1,
                executionTimeMs = duration,
                isSuccess = false
            )
        }

        // Fallback: If running basic shell commands locally without adb daemon root, run via local shell
        val localShellFallback = if (adbCommand.startsWith("shell ")) {
            adbCommand.removePrefix("shell ").trim()
        } else {
            adbCommand
        }

        val result = shellExecutor.execute(localShellFallback)
        val duration = System.currentTimeMillis() - startTime
        ToolExecutionResult(
            toolName = "adb_command",
            stdout = "[Executed via local shell to $host:$port]\n${result.stdout}",
            stderr = result.stderr,
            exitCode = result.exitCode,
            executionTimeMs = duration,
            isSuccess = result.isSuccess
        )
    }

    suspend fun testConnection(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        testSocketConnection(preferencesManager.adbHost, preferencesManager.adbPort)
    }

    private fun testSocketConnection(host: String, port: Int): Pair<Boolean, String> {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), 2500)
                Pair(true, "Successfully connected to $host:$port")
            }
        } catch (e: Exception) {
            Pair(false, e.localizedMessage ?: "Connection refused")
        }
    }

    suspend fun pairDevice(pairingPort: Int, pairingCode: String): ToolExecutionResult = withContext(Dispatchers.IO) {
        val host = preferencesManager.adbHost
        val cmd = "adb pair $host:$pairingPort $pairingCode"
        shellExecutor.execute(cmd)
    }
}
