package com.nexterm.app.terminal

import android.content.Context
import android.util.Log
import com.nexterm.app.terminal.ansi.AnsiParser
import com.nexterm.app.terminal.pty.Pty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

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

        Log.e("TerminalSessionManager", "createSession id=$id name=$name wd=$workingDirectory rows=$rows cols=$cols")

        val emulator = TerminalEmulator(rows, cols, ansiParser)
        val resolvedWorkingDirectory = if (workingDirectory.isBlank()) {
            context.filesDir.absolutePath
        } else {
            workingDirectory
        }

        val session = TerminalSession(
            id = id,
            name = name,
            context = context,
            initialWorkingDirectory = resolvedWorkingDirectory,
            emulator = emulator,
            rows = rows,
            cols = cols
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
    private val context: Context,
    initialWorkingDirectory: String,
    private val emulator: TerminalEmulator,
    rows: Int,
    cols: Int
) {
    private val _output = MutableStateFlow("")
    val output: StateFlow<String> = _output.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _title = MutableStateFlow(name)
    val title: StateFlow<String> = _title.asStateFlow()

    val screenContent = emulator.screenContent
    val cursorPosition = emulator.cursorPosition

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val pty = Pty(rows = rows, cols = cols)
    private val started = AtomicBoolean(false)

    private var currentWorkingDirectory: String = initialWorkingDirectory
    private var readerJob: Job? = null
    private var waiterJob: Job? = null

    @Volatile
    private var bootstrapActive = true

    private val promptText = "nexterm$ "

    fun start() {
        if (!started.compareAndSet(false, true)) return

        Log.e("TerminalSession", "start() session=$id")

        setupEnvironment()

        val ok = pty.startShell(
            shell = "/system/bin/sh",
            args = arrayOf("/system/bin/sh", "-"),
            environment = mapOf(
                "HOME" to context.filesDir.absolutePath,
                "PWD" to currentWorkingDirectory
            ),
            workingDirectory = currentWorkingDirectory
        )

        Log.e("TerminalSession", "pty.startShell result=$ok")

        if (!ok) {
            emulator.write("\u001B[31mFailed to start PTY shell\u001B[0m\r\n")
            return
        }

        emulator.write("\u001B[2J\u001B[H")
        startReader()
        startWaiter()
        bootstrapShell()
    }

    private fun bootstrapShell() {
        scope.launch {
            delay(150)

            val profilePath = "${context.filesDir.absolutePath}/usr/etc/profile"

            Log.e("TerminalSession", "bootstrap: sourcing profile $profilePath")
            pty.write(". \"$profilePath\"\r")
            delay(80)

            Log.e("TerminalSession", "bootstrap: cd $currentWorkingDirectory")
            pty.write("cd \"$currentWorkingDirectory\"\r")
            delay(80)

            Log.e("TerminalSession", "bootstrap: clear via printf")
            pty.write("printf '\\033[2J\\033[H'\r")
            delay(150)

            bootstrapActive = false
            _isRunning.value = false
            Log.e("TerminalSession", "bootstrap finished")
        }
    }

    private fun setupEnvironment() {
        listOf(
            File(context.filesDir, "usr"),
            File(context.filesDir, "usr/bin"),
            File(context.filesDir, "usr/lib"),
            File(context.filesDir, "usr/etc"),
            File(context.filesDir, "usr/share"),
            File(context.filesDir, "tmp"),
            File(context.filesDir, "home"),
            File(context.filesDir, "packages"),
            File(context.filesDir, ".bootstrap")
        ).forEach { dir ->
            if (!dir.exists()) {
                val created = dir.mkdirs()
                Log.e("TerminalSession", "mkdir ${dir.absolutePath} = $created")
            }
        }
    }

    private fun startReader() {
        readerJob?.cancel()
        readerJob = scope.launch {
            val buffer = ByteArray(4096)

            while (pty.isRunning()) {
                val count = pty.read(buffer)
                if (count > 0) {
                    var text = buffer.decodeToString(0, count)
                    Log.e("TerminalSession", "raw read: [$text]")

                    if (bootstrapActive) {
                        text = filterBootstrapNoise(text)
                        Log.e("TerminalSession", "filtered bootstrap read: [$text]")
                    }

                    if (text.isNotBlank()) {
                        emulator.write(text)
                        _output.value += text
                        updateWorkingDirectoryFromPromptHeuristic(text)
                        updateRunningStateFromOutput(text)
                    }
                } else if (count < 0) {
                    Log.e("TerminalSession", "reader got EOF/error")
                    break
                }
            }

            _isRunning.value = false
        }
    }

    private fun updateRunningStateFromOutput(text: String) {
        val normalized = text.replace("\r", "")
        val trimmedEnd = normalized.trimEnd()

        if (!bootstrapActive && trimmedEnd.endsWith(promptText.trim())) {
            Log.e("TerminalSession", "prompt detected -> command finished")
            _isRunning.value = false
        }
    }

    private fun filterBootstrapNoise(raw: String): String {
        val profilePath = "${context.filesDir.absolutePath}/usr/etc/profile"

        val filteredLines = raw
            .replace("\r", "")
            .split("\n")
            .map { it.trimEnd() }
            .filterNot { line ->
                val t = line.trim()

                t.isEmpty() ||
                        t == ". \"$profilePath\"" ||
                        t == "cd \"$currentWorkingDirectory\"" ||
                        t == "printf '\\033[2J\\033[H'" ||
                        t.startsWith(". \"$profilePath")
            }
            .filterNot { line ->
                val t = line.trim()
                t == "nexterm$ p" ||
                        t == "p" ||
                        t == ":/dat" ||
                        t.startsWith(":/data/user/0/") ||
                        t.startsWith("\"/data/user/0/") ||
                        t.contains("/usr/etc/profile") && t.contains("<")
            }

        val result = filteredLines.joinToString("\r\n").trim()

        return if (bootstrapActive && result == "nexterm$") {
            promptText
        } else {
            result
        }
    }

    private fun startWaiter() {
        waiterJob?.cancel()
        waiterJob = scope.launch {
            val exitCode = pty.waitFor()
            Log.e("TerminalSession", "process exited code=$exitCode")
            _isRunning.value = false
            emulator.write("\r\n\u001B[33m[process exited: $exitCode]\u001B[0m\r\n")
        }
    }

    fun resize(rows: Int, cols: Int) {
        Log.e("TerminalSession", "resize rows=$rows cols=$cols")
        emulator.resize(rows, cols)
        pty.resize(rows, cols)
    }

    fun terminate() {
        Log.e("TerminalSession", "terminate session=$id")
        readerJob?.cancel()
        waiterJob?.cancel()
        pty.destroy()
        _isRunning.value = false
        scope.cancel()
    }

    fun clear() {
        emulator.write("\u001B[2J\u001B[H")
    }

    fun writeToEmulator(text: String) {
        emulator.write(text)
    }

    fun getScrollbackHistory() = emulator.getScrollbackHistory()

    suspend fun sendInput(input: String) {
        Log.e("TerminalSession", "sendInput: [$input]")
        pty.write(input)
    }

    suspend fun sendKey(key: String) {
        Log.e("TerminalSession", "sendKey: $key")
        when (key.uppercase()) {
            "ENTER" -> pty.write("\r")
            "BACKSPACE" -> pty.write(byteArrayOf(0x7F))
            "TAB" -> pty.write("\t")
            "ESC", "ESCAPE" -> pty.write(byteArrayOf(0x1B))
            "CTRL+C" -> {
                pty.write(byteArrayOf(0x03))
                _isRunning.value = false
            }
            "CTRL+D" -> {
                pty.write(byteArrayOf(0x04))
                _isRunning.value = false
            }
            "CTRL+Z" -> {
                pty.write(byteArrayOf(0x1A))
                _isRunning.value = false
            }
            "CTRL+L" -> {
                pty.write(byteArrayOf(0x0C))
                _isRunning.value = false
            }
            "CTRL+A" -> pty.write(byteArrayOf(0x01))
            "CTRL+E" -> pty.write(byteArrayOf(0x05))
            "CTRL+U" -> pty.write(byteArrayOf(0x15))
            "CTRL+K" -> pty.write(byteArrayOf(0x0B))
            "CTRL+W" -> pty.write(byteArrayOf(0x17))
            "UP" -> pty.write("\u001B[A")
            "DOWN" -> pty.write("\u001B[B")
            "RIGHT" -> pty.write("\u001B[C")
            "LEFT" -> pty.write("\u001B[D")
            "HOME" -> pty.write("\u001B[H")
            "END" -> pty.write("\u001B[F")
            "PGUP" -> pty.write("\u001B[5~")
            "PGDN" -> pty.write("\u001B[6~")
            "DELETE", "DEL" -> pty.write("\u001B[3~")
            "INSERT", "INS" -> pty.write("\u001B[2~")
            else -> if (key.length == 1) pty.write(key)
        }
    }

    suspend fun executeCommand(command: String) {
        Log.e("TerminalSession", "executeCommand: [$command]")
        _isRunning.value = true
        pty.write(command)
        pty.write("\r")
    }

    private fun updateWorkingDirectoryFromPromptHeuristic(text: String) {
        val lines = text.split('\n')
        lines.forEach { line ->
            val trimmed = line.trim()
            if (trimmed.startsWith("/") && !trimmed.contains("  ")) {
                currentWorkingDirectory = trimmed.substringBefore(" $").trim()
                Log.e("TerminalSession", "updated working dir: $currentWorkingDirectory")
            }
        }
    }
}