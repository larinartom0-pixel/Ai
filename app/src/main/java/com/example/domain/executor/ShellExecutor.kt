package com.example.domain.executor

import android.content.Context
import com.example.data.model.ToolExecutionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

class ShellExecutor(private val context: Context) {

    private val workspaceDir: File by lazy {
        val dir = File(context.filesDir, "workspace")
        if (!dir.exists()) dir.mkdirs()
        dir
    }

    suspend fun execute(command: String, timeoutSec: Long = 25): ToolExecutionResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val stdoutBuilder = StringBuilder()
        val stderrBuilder = StringBuilder()
        var exitCode = -1

        try {
            val processBuilder = ProcessBuilder("/system/bin/sh", "-c", command)
            processBuilder.directory(workspaceDir)

            val env = processBuilder.environment()
            env["HOME"] = workspaceDir.absolutePath
            env["TMPDIR"] = context.cacheDir.absolutePath
            val existingPath = env["PATH"] ?: "/system/bin"
            env["PATH"] = "${workspaceDir.absolutePath}:/data/data/com.termux/files/usr/bin:$existingPath"

            val process = processBuilder.start()

            val stdoutJob = kotlinx.coroutines.GlobalScope.let {
                // Read stdout
                Thread {
                    try {
                        BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                            var line: String?
                            while (reader.readLine().also { line = it } != null) {
                                if (stdoutBuilder.length < 100_000) {
                                    stdoutBuilder.append(line).append("\n")
                                }
                            }
                        }
                    } catch (_: Exception) {}
                }.apply { isDaemon = true; start() }
            }

            val stderrJob = kotlinx.coroutines.GlobalScope.let {
                // Read stderr
                Thread {
                    try {
                        BufferedReader(InputStreamReader(process.errorStream)).use { reader ->
                            var line: String?
                            while (reader.readLine().also { line = it } != null) {
                                if (stderrBuilder.length < 50_000) {
                                    stderrBuilder.append(line).append("\n")
                                }
                            }
                        }
                    } catch (_: Exception) {}
                }.apply { isDaemon = true; start() }
            }

            val finished = process.waitFor(timeoutSec, TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                stderrBuilder.append("\n[Process timed out after ${timeoutSec}s]\n")
                exitCode = 124
            } else {
                exitCode = process.exitValue()
            }

            // Wait brief moment for threads to catch up
            stdoutJob.join(300)
            stderrJob.join(300)

            val duration = System.currentTimeMillis() - startTime
            val out = stdoutBuilder.toString().trim()
            val err = stderrBuilder.toString().trim()

            ToolExecutionResult(
                toolName = "execute_shell",
                stdout = if (out.isEmpty() && err.isEmpty() && exitCode == 0) "[Command executed successfully with no output]" else out,
                stderr = err,
                exitCode = exitCode,
                executionTimeMs = duration,
                isSuccess = exitCode == 0
            )
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            ToolExecutionResult(
                toolName = "execute_shell",
                stdout = "",
                stderr = "Execution error: ${e.localizedMessage}",
                exitCode = -1,
                executionTimeMs = duration,
                isSuccess = false
            )
        }
    }

    fun getWorkspacePath(): String = workspaceDir.absolutePath
}
