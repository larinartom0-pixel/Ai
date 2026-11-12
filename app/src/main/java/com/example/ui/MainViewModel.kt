package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ChatSessionEntity
import com.example.data.local.PreferencesManager
import com.example.data.model.AgentStatus
import com.example.data.model.ToolExecutionResult
import com.example.domain.agent.AgentCoordinator
import com.example.domain.executor.AdbExecutor
import com.example.domain.executor.PythonExecutor
import com.example.domain.executor.ShellExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppTab {
    AGENT,
    TERMINAL,
    ADB,
    SETTINGS
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val chatDao = db.chatDao()
    val preferencesManager = PreferencesManager(application)
    val shellExecutor = ShellExecutor(application)
    val pythonExecutor = PythonExecutor(application, shellExecutor)
    val adbExecutor = AdbExecutor(application, preferencesManager, shellExecutor)

    val agentCoordinator = AgentCoordinator(
        context = application,
        chatDao = chatDao,
        preferencesManager = preferencesManager,
        shellExecutor = shellExecutor,
        pythonExecutor = pythonExecutor,
        adbExecutor = adbExecutor
    )

    private val _currentTab = MutableStateFlow(AppTab.AGENT)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    val sessions: StateFlow<List<ChatSessionEntity>> = chatDao.getAllSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedSessionId = MutableStateFlow<Long?>(null)
    val selectedSessionId: StateFlow<Long?> = _selectedSessionId.asStateFlow()

    val currentMessages: StateFlow<List<ChatMessageEntity>> = _selectedSessionId.flatMapLatest { id ->
        if (id != null) chatDao.getMessagesForSession(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val agentStatus: StateFlow<AgentStatus> = agentCoordinator.statusFlow
    val terminalLogs: StateFlow<List<String>> = agentCoordinator.terminalLogs

    // Terminal interactive screen state
    private val _manualTerminalHistory = MutableStateFlow<List<String>>(
        listOf(
            "Terminal Session initialized.",
            "Type commands below or tap quick actions (ls, pwd, uname, python3, df, top)."
        )
    )
    val manualTerminalHistory: StateFlow<List<String>> = _manualTerminalHistory.asStateFlow()

    private val _adbStatusText = MutableStateFlow("ADB disconnected")
    val adbStatusText: StateFlow<String> = _adbStatusText.asStateFlow()

    private val _isAdbConnected = MutableStateFlow(false)
    val isAdbConnected: StateFlow<Boolean> = _isAdbConnected.asStateFlow()

    init {
        // Automatically select or create first session if none exists
        viewModelScope.launch {
            sessions.collect { list ->
                if (_selectedSessionId.value == null && list.isNotEmpty()) {
                    _selectedSessionId.value = list.first().id
                }
            }
        }
    }

    fun setTab(tab: AppTab) {
        _currentTab.value = tab
    }

    fun selectSession(sessionId: Long) {
        _selectedSessionId.value = sessionId
    }

    fun createNewSession(title: String = "New Task") {
        viewModelScope.launch {
            val newSession = ChatSessionEntity(
                title = title,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            val id = chatDao.insertSession(newSession)
            _selectedSessionId.value = id
        }
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            chatDao.deleteSessionById(sessionId)
            if (_selectedSessionId.value == sessionId) {
                _selectedSessionId.value = null
            }
        }
    }

    fun submitUserTask(prompt: String) {
        val trimmed = prompt.trim()
        if (trimmed.isEmpty()) return

        viewModelScope.launch {
            var sessionId = _selectedSessionId.value
            if (sessionId == null) {
                // Auto create session with prompt as title
                val sessionTitle = if (trimmed.length > 30) trimmed.take(27) + "..." else trimmed
                val newSession = ChatSessionEntity(
                    title = sessionTitle,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                sessionId = chatDao.insertSession(newSession)
                _selectedSessionId.value = sessionId
            }

            agentCoordinator.startTask(sessionId, trimmed)
        }
    }

    fun stopCurrentTask() {
        _selectedSessionId.value?.let { id ->
            agentCoordinator.stopTask(id)
        }
    }

    fun retryRateLimitNow() {
        _selectedSessionId.value?.let { id ->
            agentCoordinator.retryImmediately(id)
        }
    }

    // Interactive Terminal commands
    fun runManualTerminalCommand(cmd: String) {
        val trimmed = cmd.trim()
        if (trimmed.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            val history = _manualTerminalHistory.value.toMutableList()
            history.add("$ $trimmed")
            _manualTerminalHistory.value = history

            val result = shellExecutor.execute(trimmed)

            val updated = _manualTerminalHistory.value.toMutableList()
            if (result.stdout.isNotBlank()) updated.add(result.stdout)
            if (result.stderr.isNotBlank()) updated.add("Stderr: ${result.stderr}")
            updated.add("[Process completed with exit code ${result.exitCode}]")

            // Keep terminal history manageable
            if (updated.size > 300) {
                _manualTerminalHistory.value = updated.takeLast(200)
            } else {
                _manualTerminalHistory.value = updated
            }
        }
    }

    fun clearManualTerminal() {
        _manualTerminalHistory.value = listOf("Terminal cleared.")
    }

    // ADB actions
    fun testAdbConnection() {
        viewModelScope.launch {
            _adbStatusText.value = "Testing connection to ${preferencesManager.adbHost}:${preferencesManager.adbPort}..."
            val result = adbExecutor.testConnection()
            _isAdbConnected.value = result.first
            _adbStatusText.value = result.second
        }
    }

    fun runAdbCommand(cmd: String) {
        viewModelScope.launch {
            val result = adbExecutor.executeCommand(cmd)
            _adbStatusText.value = if (result.isSuccess) {
                "Command: $cmd\n${result.stdout}"
            } else {
                "Error: ${result.stderr}"
            }
        }
    }

    fun pairAdbDevice(port: Int, code: String) {
        viewModelScope.launch {
            _adbStatusText.value = "Pairing with port $port and code $code..."
            val result = adbExecutor.pairDevice(port, code)
            _adbStatusText.value = if (result.isSuccess) {
                "Pairing successful: ${result.stdout}"
            } else {
                "Pairing failed: ${result.stderr}"
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            chatDao.clearAllMessages()
            chatDao.clearAllSessions()
            _selectedSessionId.value = null
            agentCoordinator.clearTerminalLogs()
            _manualTerminalHistory.value = listOf("History cleared.")
        }
    }
}
