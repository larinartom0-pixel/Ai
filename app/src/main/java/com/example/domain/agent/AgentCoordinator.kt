package com.example.domain.agent

import android.content.Context
import com.example.data.local.ChatDao
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ChatSessionEntity
import com.example.data.local.PreferencesManager
import com.example.data.model.AgentStatus
import com.example.data.model.ToolExecutionResult
import com.example.domain.executor.AdbExecutor
import com.example.domain.executor.PythonExecutor
import com.example.domain.executor.ShellExecutor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject

class AgentCoordinator(
    private val context: Context,
    private val chatDao: ChatDao,
    private val preferencesManager: PreferencesManager,
    private val shellExecutor: ShellExecutor,
    private val pythonExecutor: PythonExecutor,
    private val adbExecutor: AdbExecutor
) {
    private val geminiApiClient = GeminiApiClient()
    private val scope = CoroutineScope(Dispatchers.Default)
    private var currentExecutionJob: Job? = null

    private val _statusFlow = MutableStateFlow<AgentStatus>(AgentStatus.Idle)
    val statusFlow: StateFlow<AgentStatus> = _statusFlow.asStateFlow()

    private val _terminalLogs = MutableStateFlow<List<String>>(emptyList())
    val terminalLogs: StateFlow<List<String>> = _terminalLogs.asStateFlow()

    fun appendTerminalLog(log: String) {
        val updated = _terminalLogs.value.toMutableList()
        updated.add(log)
        if (updated.size > 200) updated.removeAt(0)
        _terminalLogs.value = updated
    }

    fun clearTerminalLogs() {
        _terminalLogs.value = emptyList()
    }

    fun startTask(sessionId: Long, userPrompt: String) {
        currentExecutionJob?.cancel()
        currentExecutionJob = scope.launch {
            runAgentLoop(sessionId, userPrompt)
        }
    }

    fun stopTask(sessionId: Long) {
        currentExecutionJob?.cancel()
        currentExecutionJob = null
        _statusFlow.value = AgentStatus.Idle
        scope.launch {
            chatDao.getSessionById(sessionId)?.let { session ->
                chatDao.updateSession(session.copy(status = "idle", retryCountdown = 0))
            }
        }
    }

    fun retryImmediately(sessionId: Long) {
        // Force countdown to finish and immediately retry
        currentExecutionJob?.cancel()
        currentExecutionJob = scope.launch {
            runAgentLoop(sessionId, null, retryAttempt = 1)
        }
    }

    private suspend fun runAgentLoop(sessionId: Long, initialPrompt: String?, retryAttempt: Int = 0) {
        val session = chatDao.getSessionById(sessionId) ?: return

        if (!initialPrompt.isNullOrBlank()) {
            chatDao.insertMessage(
                ChatMessageEntity(
                    sessionId = sessionId,
                    role = "user",
                    content = initialPrompt
                )
            )
            chatDao.updateSession(
                session.copy(
                    updatedAt = System.currentTimeMillis(),
                    status = "running",
                    retryCountdown = 0,
                    retryAttempt = 0,
                    lastError = null
                )
            )
        }

        var attempt = retryAttempt
        val maxIterations = 15
        var currentStep = 0
        var isTaskFinished = false

        val systemPrompt = buildSystemPrompt()

        while (!isTaskFinished && currentStep < maxIterations && coroutineScopeActive()) {
            currentStep++
            _statusFlow.value = AgentStatus.Running(currentStep, "Analyzing task and context...")

            // 1. Build context from Room database
            val history = buildHistory(sessionId)

            // 2. Query Gemini
            val apiKey = preferencesManager.geminiApiKey
            val model = preferencesManager.selectedModel
            val baseUrl = preferencesManager.customBaseUrl

            appendTerminalLog("🤖 [Agent] Step $currentStep: Sending request to $model...")

            val response = geminiApiClient.generateContent(
                apiKey = apiKey,
                model = model,
                customBaseUrl = baseUrl,
                systemInstruction = systemPrompt,
                history = history
            )

            when (response) {
                is GeminiResponse.RateLimitOrBusy -> {
                    // Rate limit or server overload hit!
                    // Requirement: wait 30 seconds, display countdown, auto-retry sending what was done so far
                    val intervalSec = preferencesManager.retryIntervalSeconds.coerceAtLeast(5)
                    attempt++
                    appendTerminalLog("⚠️ [RateLimit] ${response.message}. Retrying in ${intervalSec}s (Attempt $attempt)...")

                    chatDao.updateSession(
                        session.copy(
                            status = "waiting_retry",
                            retryCountdown = intervalSec,
                            retryAttempt = attempt,
                            lastError = response.message
                        )
                    )

                    for (sec in intervalSec downTo 1) {
                        if (!coroutineScopeActive()) break
                        _statusFlow.value = AgentStatus.WaitingRetry(
                            countdownSeconds = sec,
                            attempt = attempt,
                            reason = response.message
                        )
                        chatDao.updateSession(session.copy(retryCountdown = sec))
                        delay(1000)
                    }

                    if (coroutineScopeActive()) {
                        appendTerminalLog("🔄 [Retry] Auto-retrying with preserved context window...")
                        // Continue next loop iteration, which preserves full history in Room!
                        continue
                    } else {
                        break
                    }
                }

                is GeminiResponse.FatalError -> {
                    appendTerminalLog("❌ [Fatal Error] ${response.message}")
                    _statusFlow.value = AgentStatus.Error(response.message)
                    chatDao.updateSession(
                        session.copy(
                            status = "error",
                            lastError = response.message,
                            retryCountdown = 0
                        )
                    )
                    chatDao.insertMessage(
                        ChatMessageEntity(
                            sessionId = sessionId,
                            role = "model",
                            content = "⚠️ Error: ${response.message}",
                            isError = true
                        )
                    )
                    isTaskFinished = true
                }

                is GeminiResponse.Success -> {
                    attempt = 0 // Reset attempt counter on success
                    val toolCall = response.toolCall

                    if (toolCall != null) {
                        // AI requested a tool action
                        _statusFlow.value = AgentStatus.Running(currentStep, "Executing ${toolCall.name}...")
                        appendTerminalLog("⚡ [Action] Tool: ${toolCall.name}")
                        if (toolCall.thought.isNotBlank()) {
                            appendTerminalLog("🧠 [Thought] ${toolCall.thought}")
                        }

                        val executionResult = executeTool(toolCall)

                        appendTerminalLog("📋 [Result] Exit: ${executionResult.exitCode} (${executionResult.executionTimeMs}ms)")
                        if (executionResult.stdout.isNotBlank()) {
                            appendTerminalLog("Output:\n" + executionResult.stdout.take(300))
                        }
                        if (executionResult.stderr.isNotBlank()) {
                            appendTerminalLog("Stderr:\n" + executionResult.stderr.take(200))
                        }

                        // Store tool message & result into Room database
                        chatDao.insertMessage(
                            ChatMessageEntity(
                                sessionId = sessionId,
                                role = "tool",
                                content = "Action: ${toolCall.name}\n${executionResult.formatForModel()}",
                                thought = toolCall.thought,
                                toolName = toolCall.name,
                                toolArgs = JSONObject(toolCall.arguments as Map<*, *>).toString(),
                                toolResult = executionResult.formatForModel(),
                                exitCode = executionResult.exitCode,
                                executionTimeMs = executionResult.executionTimeMs,
                                isError = !executionResult.isSuccess
                            )
                        )

                        if (toolCall.name == "task_complete") {
                            val summary = toolCall.arguments["summary"] ?: "Task successfully completed!"
                            _statusFlow.value = AgentStatus.Completed(summary)
                            chatDao.updateSession(
                                session.copy(
                                    status = "completed",
                                    updatedAt = System.currentTimeMillis(),
                                    retryCountdown = 0
                                )
                            )
                            isTaskFinished = true
                        }
                    } else {
                        // No tool call: direct text response from model
                        chatDao.insertMessage(
                            ChatMessageEntity(
                                sessionId = sessionId,
                                role = "model",
                                content = response.text
                            )
                        )
                        _statusFlow.value = AgentStatus.Completed("Finished response")
                        chatDao.updateSession(
                            session.copy(
                                status = "completed",
                                updatedAt = System.currentTimeMillis(),
                                retryCountdown = 0
                            )
                        )
                        isTaskFinished = true
                    }
                }
            }
        }

        if (!isTaskFinished && currentStep >= maxIterations) {
            val limitMsg = "Max step limit ($maxIterations) reached. You can ask the AI to continue."
            _statusFlow.value = AgentStatus.Completed(limitMsg)
            chatDao.insertMessage(
                ChatMessageEntity(
                    sessionId = sessionId,
                    role = "model",
                    content = limitMsg
                )
            )
            chatDao.updateSession(session.copy(status = "completed", retryCountdown = 0))
        }
    }

    private suspend fun executeTool(toolCall: com.example.data.model.ToolCall): ToolExecutionResult {
        return when (toolCall.name) {
            "execute_shell" -> {
                val cmd = toolCall.arguments["command"] ?: "echo 'no command'"
                shellExecutor.execute(cmd)
            }
            "run_python" -> {
                val code = toolCall.arguments["code"] ?: ""
                val filename = toolCall.arguments["filename"]
                pythonExecutor.executePythonCode(code, filename)
            }
            "install_python_lib" -> {
                val pkg = toolCall.arguments["package"] ?: ""
                pythonExecutor.installPackage(pkg)
            }
            "adb_command" -> {
                val cmd = toolCall.arguments["command"] ?: "devices"
                adbExecutor.executeCommand(cmd)
            }
            "task_complete" -> {
                val summary = toolCall.arguments["summary"] ?: "Complete"
                ToolExecutionResult("task_complete", summary, exitCode = 0, isSuccess = true)
            }
            else -> {
                // Fallback shell
                val cmd = toolCall.arguments["command"] ?: toolCall.arguments.values.firstOrNull() ?: "echo done"
                shellExecutor.execute(cmd)
            }
        }
    }

    private suspend fun buildHistory(sessionId: Long): List<ChatHistoryItem> {
        val limit = preferencesManager.contextLimit
        val allMessages = chatDao.getMessagesListForSession(sessionId)
        val selected = allMessages.takeLast(limit)

        return selected.map { msg ->
            val role = if (msg.role == "user" || msg.role == "tool") "user" else "model"
            val text = when (msg.role) {
                "tool" -> "Tool Result (${msg.toolName}):\n${msg.content}"
                else -> msg.content
            }
            ChatHistoryItem(role, text)
        }
    }

    private fun buildSystemPrompt(): String {
        return """
You are an autonomous AI Engineer & System Agent running on an Android device.
You have access to a local Linux shell workspace, Python script runner, and an ADB control interface.

Your goal is to inspect, write code, run Python scripts, install libraries if needed, execute terminal commands, test and debug, and use ADB when requested.

### AVAILABLE TOOLS:
1. `execute_shell`: Run Linux shell command in workspace.
   Arguments: {"command": "shell command here"}
2. `run_python`: Write and execute a Python script.
   Arguments: {"code": "python code here", "filename": "example.py"}
3. `install_python_lib`: Install a python package via pip.
   Arguments: {"package": "requests"}
4. `adb_command`: Execute ADB commands (e.g., input tap x y, dumpsys, pm list packages, shell commands).
   Arguments: {"command": "adb command here"}
5. `task_complete`: Mark the goal finished.
   Arguments: {"summary": "Final explanation of what was achieved and results"}

### OUTPUT FORMAT:
You MUST respond with a JSON block inside ```json ``` markdown:
```json
{
  "thought": "Brief explanation of what you plan to do next and why",
  "tool": "execute_shell | run_python | install_python_lib | adb_command | task_complete",
  "arguments": {
    "key": "value"
  }
}
```

If you don't need any tools and want to provide a direct answer, you can write normal text.
Always inspect tool output and fix any errors automatically.
""".trimIndent()
    }

    private fun coroutineScopeActive(): Boolean = scope.isActive
}
