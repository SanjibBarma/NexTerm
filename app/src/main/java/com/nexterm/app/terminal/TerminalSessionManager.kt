package com.nexterm.app.terminal

import android.content.Context
import com.nexterm.app.terminal.ansi.AnsiParser
import com.nexterm.app.terminal.shell.ShellExecutor
import com.nexterm.app.terminal.shell.ShellOutput
import com.nexterm.app.terminal.shell.SpecialKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class TerminalSessionManager(private val context: Context) {

    private val sessions = mutableMapOf<Long, TerminalSession>()
    private val _activeSessions = MutableStateFlow<List<TerminalSession>>(emptyList())
    val activeSessions: StateFlow<List<TerminalSession>> = _activeSessions.asStateFlow()

    private val _currentSession = MutableStateFlow<TerminalSession?>(null)
    val currentSession: StateFlow<TerminalSession?> = _currentSession.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private val ansiParser = AnsiParser()

    fun createSession(
        id: Long,
        name: String = "Terminal",
        workingDirectory: String = context.filesDir.absolutePath,
        rows: Int = 24,
        cols: Int = 80
    ): TerminalSession {
        val emulator = TerminalEmulator(rows, cols, ansiParser)
        val executor = ShellExecutor(context)

        val session = TerminalSession(
            id = id,
            name = name,
            workingDirectory = workingDirectory,
            emulator = emulator,
            executor = executor,
            rows = rows,
            cols = cols
        )

        sessions[id] = session
        updateSessionsList()

        // Start shell
        scope.launch {
            session.start()
        }

        return session
    }

    fun getSession(id: Long): TerminalSession? = sessions[id]

    fun setCurrentSession(id: Long) {
        _currentSession.value = sessions[id]
    }

    fun closeSession(id: Long) {
        sessions[id]?.terminate()
        sessions.remove(id)
        updateSessionsList()

        if (_currentSession.value?.id == id) {
            _currentSession.value = sessions.values.firstOrNull()
        }
    }

    fun closeAllSessions() {
        sessions.values.forEach { it.terminate() }
        sessions.clear()
        updateSessionsList()
        _currentSession.value = null
    }

    private fun updateSessionsList() {
        _activeSessions.value = sessions.values.toList()
    }
}

class TerminalSession(
    val id: Long,
    val name: String,
    val workingDirectory: String,
    private val emulator: TerminalEmulator,
    private val executor: ShellExecutor,
    private val rows: Int,
    private val cols: Int
) {
    private val _output = MutableStateFlow("")
    val output: StateFlow<String> = _output.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _title = MutableStateFlow(name)
    val title: StateFlow<String> = _title.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var collectJob: Job? = null

    private val commandMutex = Mutex()

    val screenContent = emulator.screenContent
    val cursorPosition = emulator.cursorPosition

    suspend fun start() {
        collectJob = scope.launch {
            executor.startShell(
                workingDirectory = workingDirectory,
                rows = rows,
                cols = cols
            ).collect { output ->
                when (output) {
                    is ShellOutput.Started -> {
                        _isRunning.value = true
                    }
                    is ShellOutput.Data -> {
                        emulator.write(output.text)
                        _output.value += output.text
                    }
                    is ShellOutput.Error -> {
                        val errorMsg = "\r\n\u001B[31mError: ${output.message}\u001B[0m\r\n"
                        emulator.write(errorMsg)
                    }
                    is ShellOutput.Exited -> {
                        _isRunning.value = false
                        val exitMsg = "\r\n\u001B[33m[Process completed with exit code: ${output.exitCode}]\u001B[0m\r\n"
                        emulator.write(exitMsg)
                    }
                    else -> {}
                }
            }
        }
    }

    suspend fun sendInput(input: String) {
        executor.sendInput(input)
    }

    suspend fun sendKey(key: String) {
        val specialKey = when (key.uppercase()) {
            "CTRL+C" -> SpecialKey.CTRL_C
            "CTRL+D" -> SpecialKey.CTRL_D
            "CTRL+Z" -> SpecialKey.CTRL_Z
            "CTRL+L" -> SpecialKey.CTRL_L
            "CTRL+A" -> SpecialKey.CTRL_A
            "CTRL+E" -> SpecialKey.CTRL_E
            "CTRL+U" -> SpecialKey.CTRL_U
            "CTRL+K" -> SpecialKey.CTRL_K
            "CTRL+W" -> SpecialKey.CTRL_W
            "TAB" -> SpecialKey.TAB
            "ENTER" -> SpecialKey.ENTER
            "BACKSPACE" -> SpecialKey.BACKSPACE
            "ESCAPE", "ESC" -> SpecialKey.ESCAPE
            "UP" -> SpecialKey.UP
            "DOWN" -> SpecialKey.DOWN
            "RIGHT" -> SpecialKey.RIGHT
            "LEFT" -> SpecialKey.LEFT
            "HOME" -> SpecialKey.HOME
            "END" -> SpecialKey.END
            "PGUP" -> SpecialKey.PAGE_UP
            "PGDN" -> SpecialKey.PAGE_DOWN
            "DELETE", "DEL" -> SpecialKey.DELETE
            "INSERT", "INS" -> SpecialKey.INSERT
            "F1" -> SpecialKey.F1
            "F2" -> SpecialKey.F2
            "F3" -> SpecialKey.F3
            "F4" -> SpecialKey.F4
            "F5" -> SpecialKey.F5
            "F6" -> SpecialKey.F6
            "F7" -> SpecialKey.F7
            "F8" -> SpecialKey.F8
            "F9" -> SpecialKey.F9
            "F10" -> SpecialKey.F10
            "F11" -> SpecialKey.F11
            "F12" -> SpecialKey.F12
            else -> null
        }

        if (specialKey != null) {
            executor.sendSpecialKey(specialKey)
        } else {
            // It's a regular character
            executor.sendInput(key)
        }
    }

    fun resize(rows: Int, cols: Int) {
        emulator.resize(rows, cols)
        executor.resize(rows, cols)
    }

    fun terminate() {
        collectJob?.cancel()
        executor.terminate()
        _isRunning.value = false
    }

    fun getScrollbackHistory() = emulator.getScrollbackHistory()

    fun clear() {
        emulator.write("\u001B[2J\u001B[H")
    }

    suspend fun executeCommand(command: String) {
        commandMutex.withLock {
            scope.launch {
                executor.executeCommand(command).collect { output ->
                    when (output) {
                        is ShellOutput.Data -> {
                            emulator.write(output.text)
                        }
                        is ShellOutput.Error -> {
                            val errorMsg = "\r\n\u001B[31mError: ${output.message}\u001B[0m\r\n"
                            emulator.write(errorMsg)
                        }
                        is ShellOutput.CommandCompleted -> {
                            val exitMsg = "\r\n\u001B[33m[Command completed with exit code: ${output.exitCode}]\u001B[0m\r\n"
                            emulator.write(exitMsg)
                            // Show prompt again
                            emulator.write("\u001B[32mnexterm\u001B[0m:\u001B[34m~\u001B[0m$ ")
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    fun writeToEmulator(text: String) {
        emulator.write(text)
    }
}