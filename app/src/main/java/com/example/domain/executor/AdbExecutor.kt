package com.example.domain.executor

import android.content.Context
import android.content.pm.PackageManager
import com.example.data.local.PreferencesManager
import com.example.data.model.ToolExecutionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.TimeUnit

class AdbExecutor(
    private val context: Context,
    private val preferencesManager: PreferencesManager,
    private val shellExecutor: ShellExecutor
) {

    fun isShizukuAvailable(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (_: Throwable) {
            false
        }
    }

    fun hasShizukuPermission(): Boolean {
        return try {
            if (!isShizukuAvailable()) return false
            if (Shizuku.isPreV11()) false else Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (_: Throwable) {
            false
        }
    }

    fun requestShizukuPermission(requestCode: Int = 1001) {
        try {
            if (isShizukuAvailable() && !Shizuku.isPreV11()) {
                if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                    Shizuku.requestPermission(requestCode)
                }
            }
        } catch (_: Throwable) {}
    }

    suspend fun executeCommand(adbCommand: String): ToolExecutionResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()

        // 1. Shizuku priority: If Shizuku is authorized, execute directly with elevated ADB (UID 2000)
        if (hasShizukuPermission()) {
            return@withContext executeViaShizuku(adbCommand)
        }

        // 2. If Shizuku is running but not granted yet, try to request permission
        if (isShizukuAvailable()) {
            requestShizukuPermission()
            // Wait briefly if permission was already in flight
            if (hasShizukuPermission()) {
                return@withContext executeViaShizuku(adbCommand)
            }
        }

        // 3. Check if local `adb` CLI binary is available (e.g., installed in Termux android-tools)
        val host = preferencesManager.adbHost
        val port = preferencesManager.adbPort
        val adbCheck = shellExecutor.execute("which adb || [ -f /data/data/com.termux/files/usr/bin/adb ] && echo has_adb")
        val hasAdbBinary = adbCheck.stdout.contains("adb") || adbCheck.stdout.contains("has_adb")

        if (hasAdbBinary) {
            val fullCmd = if (host.isNotBlank() && port > 0) {
                "adb connect $host:$port >/dev/null 2>&1; adb -s $host:$port $adbCommand"
            } else {
                "adb $adbCommand"
            }
            return@withContext shellExecutor.execute(fullCmd)
        }

        // 4. Test TCP socket connection to Wireless Debugging
        val socketTestResult = testSocketConnection(host, port)
        if (!socketTestResult.first) {
            val duration = System.currentTimeMillis() - startTime
            return@withContext ToolExecutionResult(
                toolName = "adb_command",
                stdout = "",
                stderr = "ADB target unreachable at $host:$port (${socketTestResult.second}).\n" +
                        "Порада: Запустіть Shizuku для миттєвого постійного доступу без повторних підключень, " +
                        "або увімкніть Wireless Debugging.",
                exitCode = 1,
                executionTimeMs = duration,
                isSuccess = false
            )
        }

        // 5. Fallback via local shell
        val localShellFallback = if (adbCommand.startsWith("shell ")) {
            adbCommand.removePrefix("shell ").trim()
        } else {
            adbCommand
        }

        val result = shellExecutor.execute(localShellFallback)
        val duration = System.currentTimeMillis() - startTime
        ToolExecutionResult(
            toolName = "adb_command",
            stdout = "[Executed via shell to $host:$port]\n${result.stdout}",
            stderr = result.stderr,
            exitCode = result.exitCode,
            executionTimeMs = duration,
            isSuccess = result.isSuccess
        )
    }

    suspend fun executeViaShizuku(rawCommand: String): ToolExecutionResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val cmd = if (rawCommand.startsWith("shell ")) rawCommand.removePrefix("shell ").trim() else rawCommand

        try {
            val newProcessMethod = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            newProcessMethod.isAccessible = true
            val process = newProcessMethod.invoke(null, arrayOf("/system/bin/sh", "-c", cmd), null, null) as Process

            val stdoutBuilder = StringBuilder()
            val stderrBuilder = StringBuilder()

            val stdoutThread = Thread {
                try {
                    BufferedReader(InputStreamReader(process.inputStream)).use { r ->
                        var line: String?
                        while (r.readLine().also { line = it } != null) {
                            if (stdoutBuilder.length < 100_000) {
                                stdoutBuilder.append(line).append("\n")
                            }
                        }
                    }
                } catch (_: Exception) {}
            }.apply { isDaemon = true; start() }

            val stderrThread = Thread {
                try {
                    BufferedReader(InputStreamReader(process.errorStream)).use { r ->
                        var line: String?
                        while (r.readLine().also { line = it } != null) {
                            if (stderrBuilder.length < 50_000) {
                                stderrBuilder.append(line).append("\n")
                            }
                        }
                    }
                } catch (_: Exception) {}
            }.apply { isDaemon = true; start() }

            val finished = process.waitFor(25, TimeUnit.SECONDS)
            val exitCode = if (finished) {
                process.exitValue()
            } else {
                process.destroyForcibly()
                stderrBuilder.append("\n[Shizuku process timed out]")
                124
            }

            stdoutThread.join(300)
            stderrThread.join(300)

            val duration = System.currentTimeMillis() - startTime
            val out = stdoutBuilder.toString().trim()
            val err = stderrBuilder.toString().trim()

            ToolExecutionResult(
                toolName = "adb_shizuku",
                stdout = if (out.isEmpty() && err.isEmpty() && exitCode == 0) "[Shizuku execution success with no output]" else out,
                stderr = err,
                exitCode = exitCode,
                executionTimeMs = duration,
                isSuccess = exitCode == 0
            )
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            ToolExecutionResult(
                toolName = "adb_shizuku",
                stdout = "",
                stderr = "Shizuku error: ${e.localizedMessage}",
                exitCode = -1,
                executionTimeMs = duration,
                isSuccess = false
            )
        }
    }

    suspend fun testConnection(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (hasShizukuPermission()) {
            return@withContext Pair(true, "🟢 Shizuku ADB активно (UID 2000 Shell) - постійний доступ без повторних спарювань!")
        }
        if (isShizukuAvailable()) {
            return@withContext Pair(false, "🟡 Shizuku запущено, але потрібен дозвіл. Натисніть 'Запросити дозвіл Shizuku'.")
        }
        testSocketConnection(preferencesManager.adbHost, preferencesManager.adbPort)
    }

    private fun testSocketConnection(host: String, port: Int): Pair<Boolean, String> {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), 2500)
                Pair(true, "Підключено до Wireless ADB $host:$port")
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
