package com.nexterm.app.ui.screens.terminal

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexterm.app.data.local.preferences.TerminalSettings
import com.nexterm.app.data.repository.SessionRepository
import com.nexterm.app.data.repository.SettingsRepository
import com.nexterm.app.terminal.TerminalSession
import com.nexterm.app.terminal.TerminalSessionManager
import com.nexterm.app.terminal.buffer.TerminalLine
import com.nexterm.app.package_manager.PackageManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TerminalViewModel(
    private val sessionManager: TerminalSessionManager,
    private val sessionRepository: SessionRepository,
    private val settingsRepository: SettingsRepository,
    private val packageManager: PackageManager
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

    private val _isCurrentSessionRunning = MutableStateFlow(false)
    val isCurrentSessionRunning: StateFlow<Boolean> = _isCurrentSessionRunning.asStateFlow()

    private var historyIndex = -1
    private var screenContentJob: Job? = null
    private var restoreJob: Job? = null
    private var runningStateJob: Job? = null

    init {
        Log.d("NexTerm-LOG", "TerminalViewModel Initialized")
        viewModelScope.launch {
            loadCommandHistory()
        }
        restoreSessionsOnLaunch()
    }

    private fun restoreSessionsOnLaunch() {
        if (restoreJob != null) return

        restoreJob = viewModelScope.launch {
            val savedSessions = sessionRepository.getActiveSessions().first()

            if (savedSessions.isEmpty()) {
                createSession()
                return@launch
            }

            val restoredSessions = savedSessions.map { entity ->
                sessionManager.createSession(
                    id = entity.id,
                    name = entity.name,
                    workingDirectory = entity.workingDirectory.ifBlank {
                        sessionManager.getDefaultWorkingDirectory()
                    }
                )
            }

            val firstSession = restoredSessions.firstOrNull()
            _currentSession.value = firstSession

            firstSession?.let {
                sessionManager.setCurrentSession(it.id)
                observeSessionScreenContent(it)
                observeRunningState(it)
                sessionRepository.updateLastAccessed(it.id)
            }
        }
    }

    fun createSession(name: String = generateNextSessionName()) {
        viewModelScope.launch {
            val workingDirectory = sessionManager.getDefaultWorkingDirectory()
            val sessionId = sessionRepository.createSession(name = name, workingDirectory = workingDirectory)
            val session = sessionManager.createSession(id = sessionId, name = name, workingDirectory = workingDirectory)

            _currentSession.value = session
            sessionManager.setCurrentSession(session.id)
            observeSessionScreenContent(session)
            observeRunningState(session)
            sessionRepository.updateLastAccessed(session.id)
        }
    }

    fun switchSession(session: TerminalSession) {
        if (_currentSession.value?.id == session.id) return
        _currentSession.value = session
        sessionManager.setCurrentSession(session.id)
        viewModelScope.launch { sessionRepository.updateLastAccessed(session.id) }
        observeSessionScreenContent(session)
        observeRunningState(session)
    }

    fun closeSession(session: TerminalSession) {
        viewModelScope.launch {
            val currentSessions = sessions.value
            if (currentSessions.size <= 1) return@launch
            val closingIndex = currentSessions.indexOfFirst { it.id == session.id }
            val isClosingCurrent = _currentSession.value?.id == session.id

            sessionManager.closeSession(session.id)
            sessionRepository.deleteSession(session.id)

            val remainingSessions = sessions.value
            if (isClosingCurrent) {
                val nextSession = remainingSessions.getOrNull(closingIndex) ?: remainingSessions.lastOrNull()
                _currentSession.value = nextSession
                nextSession?.let {
                    sessionManager.setCurrentSession(it.id)
                    observeSessionScreenContent(it)
                    observeRunningState(it)
                    sessionRepository.updateLastAccessed(it.id)
                }
            }
        }
    }

    fun sendInput(input: String) {
        viewModelScope.launch { _currentSession.value?.sendInput(input) }
    }

    fun sendKey(key: String) {
        viewModelScope.launch { _currentSession.value?.sendKey(key) }
    }

    fun onCommand(command: String) {
        val trimmed = command.trim()
        if (trimmed.isEmpty()) return

        viewModelScope.launch {
            val session = _currentSession.value ?: return@launch

            // Log to Android Studio Logcat Only (Terminal remains clean)
            Log.d("NexTerm-LOG", ">>> [COMMAND]: $trimmed")

            if (trimmed.startsWith("pkg install ") || trimmed.startsWith("pkg i ")) {
                _isCurrentSessionRunning.value = true
                val pkgName = trimmed.substringAfter("install ").substringAfter("i ").trim()
                session.writeToEmulator("\u001B[33m[*] Installing $pkgName...\u001B[0m\r\n")
                
                val result = packageManager.install(pkgName)
                if (result.isSuccess) {
                    session.writeToEmulator("\u001B[32m[+] $pkgName installed.\u001B[0m\r\n")
                    Log.d("NexTerm-LOG", "[PKG SUCCESS]: $pkgName ready.")
                } else {
                    session.writeToEmulator("\u001B[31m[!] Error: ${result.exceptionOrNull()?.message}\u001B[0m\r\n")
                    Log.e("NexTerm-LOG", "[PKG ERROR]: ${result.exceptionOrNull()?.message}")
                }
                session.sendKey("ENTER")
                _isCurrentSessionRunning.value = false
                return@launch
            }

            val parts = trimmed.split(" ")
            val cmdBase = parts[0]
            val binFile = java.io.File(sessionManager.getDefaultWorkingDirectory(), "usr/bin/$cmdBase")
            val finalCommand = if (binFile.exists()) "sh ${binFile.absolutePath} ${trimmed.substringAfter(cmdBase, "").trim()}" else trimmed

            Log.d("NexTerm-LOG", "[EXECUTING]: $finalCommand")
            session.executeCommand(finalCommand)

            sessionRepository.addCommandToHistory(session.id, trimmed, "", 0, 0)
            _commandHistory.value = (_commandHistory.value + trimmed).distinct()
            historyIndex = -1
        }
    }

    fun resize(rows: Int, cols: Int) {
        _currentSession.value?.resize(rows, cols)
        viewModelScope.launch { settingsRepository.updateTerminalSize(rows, cols) }
    }

    fun getPreviousCommand(): String? {
        val history = _commandHistory.value
        if (history.isEmpty()) return null
        historyIndex = if (historyIndex == -1) history.lastIndex else (historyIndex - 1).coerceAtLeast(0)
        return history.getOrNull(historyIndex)
    }

    fun getNextCommand(): String? {
        val history = _commandHistory.value
        if (history.isEmpty() || historyIndex == -1) return null
        historyIndex++
        return if (historyIndex > history.lastIndex) { historyIndex = -1; null } else history.getOrNull(historyIndex)
    }

    fun resetHistoryNavigation() { historyIndex = -1 }

    private fun observeSessionScreenContent(session: TerminalSession) {
        screenContentJob?.cancel()
        screenContentJob = viewModelScope.launch {
            session.screenContent.collect { content ->
                _screenContent.value = content.map { it.copy() }
            }
        }
    }

    private fun observeRunningState(session: TerminalSession) {
        runningStateJob?.cancel()
        runningStateJob = viewModelScope.launch {
            session.isRunning.collect { running -> _isCurrentSessionRunning.value = running }
        }
    }

    private suspend fun loadCommandHistory() {
        _commandHistory.value = sessionRepository.getUniqueCommands(100)
    }

    private fun generateNextSessionName(): String {
        val regex = Regex("""^Terminal(?:\s+(\d+))?$""", RegexOption.IGNORE_CASE)
        val maxNumber = sessions.value.mapNotNull { session ->
            val match = regex.matchEntire(session.name.trim()) ?: return@mapNotNull null
            match.groupValues.getOrNull(1)?.toIntOrNull() ?: 1
        }.maxOrNull() ?: 0
        return if (maxNumber == 0) "Terminal 1" else "Terminal ${maxNumber + 1}"
    }

    override fun onCleared() {
        super.onCleared()
        screenContentJob?.cancel()
        runningStateJob?.cancel()
        restoreJob?.cancel()
        sessionManager.closeAllSessions()
    }
}
