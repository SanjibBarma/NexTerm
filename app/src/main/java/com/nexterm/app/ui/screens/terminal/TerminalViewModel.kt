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
        Log.e("TerminalViewModel", "init")
        viewModelScope.launch {
            loadCommandHistory()
        }
        restoreSessionsOnLaunch()
    }

    private fun restoreSessionsOnLaunch() {
        if (restoreJob != null) return

        restoreJob = viewModelScope.launch {
            Log.e("TerminalViewModel", "restoreSessionsOnLaunch()")
            val savedSessions = sessionRepository.getActiveSessions().first()

            if (savedSessions.isEmpty()) {
                Log.e("TerminalViewModel", "No saved sessions, creating new one")
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
            Log.e("TerminalViewModel", "createSession name=$name")

            val workingDirectory = sessionManager.getDefaultWorkingDirectory()

            val sessionId = sessionRepository.createSession(
                name = name,
                workingDirectory = workingDirectory
            )

            val session = sessionManager.createSession(
                id = sessionId,
                name = name,
                workingDirectory = workingDirectory
            )

            _currentSession.value = session
            sessionManager.setCurrentSession(session.id)
            observeSessionScreenContent(session)
            observeRunningState(session)
            sessionRepository.updateLastAccessed(session.id)
        }
    }

    fun switchSession(session: TerminalSession) {
        if (_currentSession.value?.id == session.id) return

        Log.e("TerminalViewModel", "switchSession id=${session.id}")

        _currentSession.value = session
        sessionManager.setCurrentSession(session.id)

        viewModelScope.launch {
            sessionRepository.updateLastAccessed(session.id)
        }

        observeSessionScreenContent(session)
        observeRunningState(session)
    }

    fun closeSession(session: TerminalSession) {
        viewModelScope.launch {
            Log.e("TerminalViewModel", "closeSession id=${session.id}")

            val currentSessions = sessions.value
            if (currentSessions.size <= 1) return@launch

            val closingIndex = currentSessions.indexOfFirst { it.id == session.id }
            val isClosingCurrent = _currentSession.value?.id == session.id

            sessionManager.closeSession(session.id)
            sessionRepository.deleteSession(session.id)

            val remainingSessions = sessions.value

            if (isClosingCurrent) {
                val nextSession = when {
                    remainingSessions.isEmpty() -> null
                    closingIndex in remainingSessions.indices -> remainingSessions[closingIndex]
                    else -> remainingSessions.lastOrNull()
                }

                _currentSession.value = nextSession

                if (nextSession != null) {
                    sessionManager.setCurrentSession(nextSession.id)
                    observeSessionScreenContent(nextSession)
                    observeRunningState(nextSession)
                    sessionRepository.updateLastAccessed(nextSession.id)
                } else {
                    screenContentJob?.cancel()
                    runningStateJob?.cancel()
                    _screenContent.value = emptyList()
                    _isCurrentSessionRunning.value = false
                }
            }
        }
    }

    fun sendInput(input: String) {
        viewModelScope.launch {
            Log.e("TerminalViewModel", "sendInput: [$input]")
            _currentSession.value?.sendInput(input)
        }
    }

    fun sendKey(key: String) {
        viewModelScope.launch {
            Log.e("TerminalViewModel", "sendKey: $key")
            _currentSession.value?.sendKey(key)
        }
    }

    fun executeCommand(command: String) {
        val trimmed = command.trim()
        if (trimmed.isEmpty()) return

        Log.e("NexTerm-LOG", ">>> [NexTerm-COMMAND]: $trimmed")

        viewModelScope.launch {
            val session = _currentSession.value ?: return@launch

            // REAL PKG INTERCEPTION
            if (trimmed.startsWith("pkg install ") || trimmed.startsWith("pkg i ")) {
                _isCurrentSessionRunning.value = true
                val pkgName = trimmed.substringAfter("install ").substringAfter("i ").trim()
                session.writeToEmulator("\r\n\u001B[33m[*] NexTerm: Initializing installation for $pkgName...\u001B[0m\r\n")
                
                val result = packageManager.install(pkgName)
                if (result.isSuccess) {
                    session.writeToEmulator("\u001B[32m[+] $pkgName is now functional!\u001B[0m\r\n")
                } else {
                    session.writeToEmulator("\u001B[31m[!] Error: ${result.exceptionOrNull()?.message}\u001B[0m\r\n")
                }
                session.sendKey("ENTER")
                _isCurrentSessionRunning.value = false
                return@launch
            }

            Log.e("TerminalViewModel", "executeCommand: $trimmed")
            session.executeCommand(trimmed)

            sessionRepository.addCommandToHistory(
                sessionId = session.id,
                command = trimmed,
                output = "",
                exitCode = 0,
                executionTimeMs = 0
            )

            _commandHistory.value = (_commandHistory.value + trimmed).distinct()
            historyIndex = -1
            sessionRepository.updateLastAccessed(session.id)
        }
    }

    fun resize(rows: Int, cols: Int) {
        Log.e("TerminalViewModel", "resize rows=$rows cols=$cols")
        _currentSession.value?.resize(rows, cols)
        viewModelScope.launch {
            settingsRepository.updateTerminalSize(rows, cols)
        }
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

        historyIndex++

        return if (historyIndex > history.lastIndex) {
            historyIndex = -1
            null
        } else {
            history.getOrNull(historyIndex)
        }
    }

    fun resetHistoryNavigation() {
        historyIndex = -1
    }

    private fun observeSessionScreenContent(session: TerminalSession) {
        Log.e("TerminalViewModel", "observeSessionScreenContent session=${session.id}")
        screenContentJob?.cancel()
        screenContentJob = viewModelScope.launch {
            session.screenContent.collect { content ->
                val snapshot = content.map { it.copy() }

                snapshot.forEachIndexed { index, line ->
                    Log.e("TerminalViewModel", "VM line[$index]=${line.getText()}")
                }

                _screenContent.value = snapshot
            }
        }
    }

    private fun observeRunningState(session: TerminalSession) {
        Log.e("TerminalViewModel", "observeRunningState session=${session.id}")
        runningStateJob?.cancel()
        runningStateJob = viewModelScope.launch {
            session.isRunning.collect { running ->
                Log.e("TerminalViewModel", "session ${session.id} running=$running")
                _isCurrentSessionRunning.value = running
            }
        }
    }

    private suspend fun loadCommandHistory() {
        val commands = sessionRepository.getUniqueCommands(100)
        Log.e("TerminalViewModel", "loadCommandHistory size=${commands.size}")
        _commandHistory.value = commands
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
        Log.e("TerminalViewModel", "onCleared")
        screenContentJob?.cancel()
        runningStateJob?.cancel()
        restoreJob?.cancel()
        sessionManager.closeAllSessions()
    }
}
