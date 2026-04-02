package com.nexterm.app.ui.screens.terminal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexterm.app.data.local.preferences.TerminalSettings
import com.nexterm.app.data.repository.SessionRepository
import com.nexterm.app.data.repository.SettingsRepository
import com.nexterm.app.terminal.TerminalSession
import com.nexterm.app.terminal.TerminalSessionManager
import com.nexterm.app.terminal.buffer.TerminalLine
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TerminalViewModel(
    private val sessionManager: TerminalSessionManager,
    private val sessionRepository: SessionRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val settings: StateFlow<TerminalSettings> = settingsRepository.terminalSettings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = TerminalSettings()
        )

    private val _currentSession = MutableStateFlow<TerminalSession?>(null)
    val currentSession: StateFlow<TerminalSession?> = _currentSession.asStateFlow()

    val sessions: StateFlow<List<TerminalSession>> = sessionManager.activeSessions

    private val _screenContent = MutableStateFlow<List<TerminalLine>>(emptyList())
    val screenContent: StateFlow<List<TerminalLine>> = _screenContent.asStateFlow()

    private val _commandHistory = MutableStateFlow<List<String>>(emptyList())
    val commandHistory: StateFlow<List<String>> = _commandHistory.asStateFlow()

    private var historyIndex = -1

    init {
        viewModelScope.launch {
            loadCommandHistory()
        }
    }

    fun createSession(name: String = "Terminal") {
        viewModelScope.launch {
            val sessionId = sessionRepository.createSession(
                name = name,
                workingDirectory = ""
            )
            val session = sessionManager.createSession(
                id = sessionId,
                name = name
            )
            _currentSession.value = session

            session.screenContent.collect { content ->
                _screenContent.value = content
            }

            // Show initial prompt
            session.writeToEmulator("\u001B[32mnexterm\u001B[0m:\u001B[34m~\u001B[0m$ ")
        }
    }

    fun switchSession(session: TerminalSession) {
        _currentSession.value = session
        viewModelScope.launch {
            session.screenContent.collect { content ->
                _screenContent.value = content
            }
        }
    }

    fun closeSession(session: TerminalSession) {
        viewModelScope.launch {
            sessionManager.closeSession(session.id)
            sessionRepository.deleteSession(session.id)

            if (_currentSession.value?.id == session.id) {
                _currentSession.value = sessions.value.firstOrNull()
            }
        }
    }

    fun sendInput(input: String) {
        viewModelScope.launch {
            _currentSession.value?.sendInput(input)
        }
    }

    fun sendKey(key: String) {
        viewModelScope.launch {
            _currentSession.value?.sendKey(key)
        }
    }

    fun executeCommand(command: String) {
        viewModelScope.launch {
            _currentSession.value?.let { session ->
                // Display the command
                session.writeToEmulator("$command\n")
                // Execute it
                session.executeCommand(command)
            }

            // Add to history
            val sessionId = _currentSession.value?.id ?: return@launch
            sessionRepository.addCommandToHistory(
                sessionId = sessionId,
                command = command,
                output = "",
                exitCode = 0,
                executionTimeMs = 0
            )

            _commandHistory.value = _commandHistory.value + command
            historyIndex = -1
        }
    }

    fun resize(rows: Int, cols: Int) {
        _currentSession.value?.resize(rows, cols)
    }

    fun getPreviousCommand(): String? {
        val history = _commandHistory.value
        if (history.isEmpty()) return null

        historyIndex = if (historyIndex == -1) {
            history.lastIndex
        } else {
            (historyIndex - 1).coerceAtLeast(0)
        }

        return history.getOrNull(historyIndex)
    }

    fun getNextCommand(): String? {
        val history = _commandHistory.value
        if (history.isEmpty() || historyIndex == -1) return null

        historyIndex = (historyIndex + 1).coerceAtMost(history.lastIndex)
        return history.getOrNull(historyIndex)
    }

    private suspend fun loadCommandHistory() {
        val commands = sessionRepository.getUniqueCommands(100)
        _commandHistory.value = commands
    }

    override fun onCleared() {
        super.onCleared()
        sessionManager.closeAllSessions()
    }
}