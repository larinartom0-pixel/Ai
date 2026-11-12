package com.example.data.model

sealed interface AgentStatus {
    object Idle : AgentStatus
    data class Running(val step: Int, val action: String) : AgentStatus
    data class WaitingRetry(
        val countdownSeconds: Int,
        val attempt: Int,
        val reason: String
    ) : AgentStatus
    data class Completed(val summary: String) : AgentStatus
    data class Error(val message: String) : AgentStatus
}

data class ToolCall(
    val name: String,
    val thought: String,
    val arguments: Map<String, String>
)

data class ToolExecutionResult(
    val toolName: String,
    val stdout: String,
    val stderr: String = "",
    val exitCode: Int = 0,
    val executionTimeMs: Long = 0L,
    val isSuccess: Boolean = true
) {
    fun formatForModel(): String {
        val sb = StringBuilder()
        sb.append("Exit Code: $exitCode (Time: ${executionTimeMs}ms)\n")
        if (stdout.isNotBlank()) {
            sb.append("Output:\n").append(stdout.take(4000)).append("\n")
            if (stdout.length > 4000) sb.append("... [Output truncated]\n")
        }
        if (stderr.isNotBlank()) {
            sb.append("Errors/Stderr:\n").append(stderr.take(2000)).append("\n")
        }
        return sb.toString().trim()
    }
}
