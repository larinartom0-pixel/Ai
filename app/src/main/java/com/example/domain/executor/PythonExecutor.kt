package com.example.domain.executor

import android.content.Context
import com.example.data.model.ToolExecutionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class PythonExecutor(
    private val context: Context,
    private val shellExecutor: ShellExecutor
) {
    private val scriptsDir: File by lazy {
        val dir = File(context.filesDir, "scripts")
        if (!dir.exists()) dir.mkdirs()
        dir
    }

    suspend fun executePythonCode(code: String, customFilename: String? = null): ToolExecutionResult = withContext(Dispatchers.IO) {
        val fileName = customFilename?.takeIf { it.isNotBlank() } ?: "script_${System.currentTimeMillis()}.py"
        val scriptFile = File(scriptsDir, fileName)

        try {
            scriptFile.writeText(code)

            // Check python availability:
            // 1. Check system path for python3 / python
            // 2. Check Termux path: /data/data/com.termux/files/usr/bin/python3
            val probe = shellExecutor.execute("which python3 || which python || [ -f /data/data/com.termux/files/usr/bin/python3 ] && echo termux_python")
            val hasSystemPython = probe.stdout.contains("python")
            val hasTermuxPython = probe.stdout.contains("termux_python") || File("/data/data/com.termux/files/usr/bin/python3").exists()

            val runCmd = when {
                hasTermuxPython -> {
                    "LD_LIBRARY_PATH=/data/data/com.termux/files/usr/lib PATH=/data/data/com.termux/files/usr/bin:\$PATH python3 ${scriptFile.absolutePath}"
                }
                hasSystemPython -> {
                    "python3 ${scriptFile.absolutePath} 2>&1 || python ${scriptFile.absolutePath}"
                }
                else -> {
                    // Try direct run
                    "python3 ${scriptFile.absolutePath}"
                }
            }

            val result = shellExecutor.execute(runCmd)

            if (!hasSystemPython && !hasTermuxPython && (result.exitCode == 127 || result.stderr.contains("not found"))) {
                // Informative fallback: explain Python file was created successfully and provide one-tap install or simulated runner
                return@withContext ToolExecutionResult(
                    toolName = "run_python",
                    stdout = "Python code saved to: ${scriptFile.name}\n\nNotice: System python binary not detected in default path.\nIf Termux is installed, install Python by running:\n  pkg update && pkg install python\n\nCode preview:\n${code.lines().take(10).joinToString("\n")}\n${if (code.lines().size > 10) "... (more)" else ""}",
                    stderr = "python3: command not found (Termux or local python3 environment required)",
                    exitCode = 1,
                    executionTimeMs = result.executionTimeMs,
                    isSuccess = false
                )
            }

            return@withContext ToolExecutionResult(
                toolName = "run_python",
                stdout = "Script: ${scriptFile.name}\n${result.stdout}",
                stderr = result.stderr,
                exitCode = result.exitCode,
                executionTimeMs = result.executionTimeMs,
                isSuccess = result.isSuccess
            )

        } catch (e: Exception) {
            ToolExecutionResult(
                toolName = "run_python",
                stdout = "",
                stderr = "Python execution error: ${e.localizedMessage}",
                exitCode = -1,
                executionTimeMs = 0L,
                isSuccess = false
            )
        }
    }

    suspend fun installPackage(packageName: String): ToolExecutionResult = withContext(Dispatchers.IO) {
        val cmd = "pip install $packageName || /data/data/com.termux/files/usr/bin/pip install $packageName"
        shellExecutor.execute(cmd, timeoutSec = 60)
    }

    fun getScriptsDirectory(): String = scriptsDir.absolutePath
}
