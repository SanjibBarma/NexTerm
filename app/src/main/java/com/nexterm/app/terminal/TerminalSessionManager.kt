package com.nexterm.app.terminal

import android.content.Context
import com.nexterm.app.terminal.ansi.AnsiParser
import com.nexterm.app.terminal.shell.ShellExecutor
import com.nexterm.app.terminal.shell.ShellOutput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class TerminalSessionManager(private val context: Context) {

    private val sessions = linkedMapOf<Long, TerminalSession>()

    private val _activeSessions = MutableStateFlow<List<TerminalSession>>(emptyList())
    val activeSessions: StateFlow<List<TerminalSession>> = _activeSessions.asStateFlow()

    private val _currentSession = MutableStateFlow<TerminalSession?>(null)
    val currentSession: StateFlow<TerminalSession?> = _currentSession.asStateFlow()

    private val ansiParser = AnsiParser()

    fun getDefaultWorkingDirectory(): String = context.filesDir.absolutePath

    fun createSession(
        id: Long,
        name: String = "Terminal",
        workingDirectory: String = context.filesDir.absolutePath,
        rows: Int = 24,
        cols: Int = 80
    ): TerminalSession {
        sessions[id]?.let { existing ->
            _currentSession.value = existing
            updateSessionsList()
            return existing
        }

        val emulator = TerminalEmulator(rows, cols, ansiParser)
        val executor = ShellExecutor(context)

        val resolvedWorkingDirectory = if (workingDirectory.isBlank()) {
            context.filesDir.absolutePath
        } else {
            workingDirectory
        }

        val session = TerminalSession(
            id = id,
            name = name,
            initialWorkingDirectory = resolvedWorkingDirectory,
            emulator = emulator,
            executor = executor
        )

        sessions[id] = session
        _currentSession.value = session
        updateSessionsList()

        session.start()

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
    initialWorkingDirectory: String,
    private val emulator: TerminalEmulator,
    private val executor: ShellExecutor
) {
    private val _output = MutableStateFlow("")
    val output: StateFlow<String> = _output.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _title = MutableStateFlow(name)
    val title: StateFlow<String> = _title.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + Job())

    val screenContent = emulator.screenContent
    val cursorPosition = emulator.cursorPosition

    private var currentWorkingDirectory: String = initialWorkingDirectory

    fun start() {
        writeBanner()
        writePrompt()
    }

    fun resize(rows: Int, cols: Int) {
        emulator.resize(rows, cols)
    }

    fun terminate() {
        executor.interruptCurrentProcess()
        _isRunning.value = false
    }

    fun clear() {
        emulator.write("\u001B[2J\u001B[H")
    }

    fun writeToEmulator(text: String) {
        emulator.write(text)
    }

    fun getScrollbackHistory() = emulator.getScrollbackHistory()

    suspend fun sendInput(input: String) {
        writeToEmulator(input)
    }

    suspend fun sendKey(key: String) {
        when (key.uppercase()) {
            "ENTER" -> {
                writeToEmulator("\r\n")
                writePrompt()
            }

            "BACKSPACE" -> {
                writeToEmulator("\b \b")
            }

            "CTRL+L" -> {
                clear()
                writePrompt()
            }

            "CTRL+C" -> {
                val interrupted = executor.interruptCurrentProcess()
                emulator.write("^C\r\n")
                _isRunning.value = false
                if (interrupted) {
                    emulator.write("\r\n")
                }
                writePrompt()
            }

            "CTRL+D" -> {
                emulator.write("exit\r\n")
                _isRunning.value = false
            }
        }
    }

    suspend fun executeCommand(command: String) {
        val trimmed = command.trim()

        if (trimmed.isEmpty()) {
            writePrompt()
            return
        }

        writeCommandLine(trimmed)

        if (handleBuiltInCommand(trimmed)) {
            _isRunning.value = false
            if (trimmed != "clear") {
                emulator.write("\r\n")
            }
            writePrompt()
            return
        }

        scope.launch {
            executor.executeCommand(
                command = trimmed,
                workingDirectory = currentWorkingDirectory
            ).collect { result ->
                when (result) {
                    is ShellOutput.Started -> {
                        _isRunning.value = true
                    }

                    is ShellOutput.Data -> {
                        emulator.write(result.text)
                        _output.value += result.text
                    }

                    is ShellOutput.Error -> {
                        _isRunning.value = false
                        emulator.write("\u001B[31mError: ${result.message}\u001B[0m\r\n")
                        emulator.write("\r\n")
                        writePrompt()
                    }

                    is ShellOutput.Completed -> {
                        _isRunning.value = false
                        if (result.exitCode != 0 && result.exitCode != 143) {
                            emulator.write(
                                "\u001B[33m[exit code: ${result.exitCode}]\u001B[0m\r\n"
                            )
                        }
                        emulator.write("\r\n")
                        writePrompt()
                    }
                }
            }
        }
    }

    private fun writeBanner() {
        emulator.write("\u001B[2J\u001B[H")
        emulator.write("\u001B[36m╔═══════════════════════════════╗\u001B[0m\r\n")
        emulator.write("\u001B[36m║\u001B[0m       \u001B[1;32mWelcome to NexTerm\u001B[0m            \u001B[36m║\u001B[0m\r\n")
        emulator.write("\u001B[36m║\u001B[0m   \u001B[90mProfessional Android Terminal\u001B[0m     \u001B[36m║\u001B[0m\r\n")
        emulator.write("\u001B[36m╚═══════════════════════════════╝\u001B[0m\r\n")
        emulator.write("\r\n")
        emulator.write("\u001B[90mType 'help' to see available commands.\u001B[0m\r\n")
        emulator.write("\r\n")
    }

    private fun writePrompt() {
        val displayPath = currentWorkingDirectory.replace(executor.getHomeDirectory(), "~")
        emulator.write("\u001B[32mnexterm\u001B[0m:\u001B[34m$displayPath\u001B[0m$ ")
    }

    private fun writeCommandLine(command: String) {
        emulator.write("$command\r\n")
    }

    private fun handleBuiltInCommand(command: String): Boolean {
        return when {
            command == "clear" -> {
                clear()
                true
            }

            command == "pwd" -> {
                emulator.write("$currentWorkingDirectory\r\n")
                true
            }

            command == "help" -> {
                emulator.write("\u001B[1;32mNexTerm Help\u001B[0m\r\n")
                emulator.write("\u001B[90m────────────────────────────────────\u001B[0m\r\n")
                emulator.write("Built-in:\r\n")
                emulator.write("  clear, pwd, cd, help, exit\r\n")
                emulator.write("\r\n")
                emulator.write("System commands:\r\n")
                emulator.write("  ls, cat, mkdir, rm, touch, cp, mv, uname, date\r\n")
                emulator.write("\r\n")
                true
            }

            command == "exit" -> {
                emulator.write("logout\r\n")
                _isRunning.value = false
                true
            }

            command == "cd" -> {
                currentWorkingDirectory = executor.getHomeDirectory()
                true
            }

            command.startsWith("cd ") -> {
                val target = command.removePrefix("cd ").trim()
                val newDir = resolveDirectory(target)

                if (newDir != null) {
                    currentWorkingDirectory = newDir.absolutePath
                } else {
                    emulator.write("cd: no such file or directory: $target\r\n")
                }
                true
            }

            else -> false
        }
    }

    private fun resolveDirectory(path: String): File? {
        val targetFile = when {
            path == "~" -> File(executor.getHomeDirectory())
            path.startsWith("/") -> File(path)
            else -> File(currentWorkingDirectory, path)
        }

        return if (targetFile.exists() && targetFile.isDirectory) {
            targetFile
        } else {
            null
        }
    }
}