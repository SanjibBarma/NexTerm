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

class TerminalSessionManager(
    private val context: Context,
    private val bootstrapManager: com.nexterm.app.package_manager.BootstrapManager
) {

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
        val session = TerminalSession(
            id = id,
            name = name,
            context = context,
            initialWorkingDirectory = workingDirectory,
            emulator = emulator,
            rows = rows,
            cols = cols,
            bootstrapManager = bootstrapManager
        )

        sessions[id] = session
        _currentSession.value = session
        updateSessionsList()
        session.start()

        return session
    }

    fun getSession(id: Long): TerminalSession? = sessions[id]
    fun setCurrentSession(id: Long) { _currentSession.value = sessions[id] }
    fun closeSession(id: Long) { sessions[id]?.terminate(); sessions.remove(id); updateSessionsList() }
    fun closeAllSessions() { sessions.values.forEach { it.terminate() }; sessions.clear(); updateSessionsList() }
    private fun updateSessionsList() { _activeSessions.value = sessions.values.toList() }
}

class TerminalSession(
    val id: Long,
    val name: String,
    private val context: Context,
    initialWorkingDirectory: String,
    private val emulator: TerminalEmulator,
    rows: Int,
    cols: Int,
    private val bootstrapManager: com.nexterm.app.package_manager.BootstrapManager
) {
    private val _output = MutableStateFlow("")
    val output: StateFlow<String> = _output.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

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

    // Hacker Prompt Pattern
    private val promptPattern = Regex(".*#\\s*$")

    fun start() {
        if (!started.compareAndSet(false, true)) return
        setupEnvironment()
        val ok = pty.startShell(
            shell = "/system/bin/sh",
            args = arrayOf("/system/bin/sh", "-"),
            environment = mapOf("HOME" to context.filesDir.absolutePath, "PWD" to currentWorkingDirectory),
            workingDirectory = currentWorkingDirectory
        )
        if (!ok) {
            emulator.write("\u001B[31mFailed to start shell\u001B[0m\r\n")
            return
        }
        emulator.write("\u001B[2J\u001B[H")
        startReader()
        startWaiter()
        bootstrapShell()
    }

    companion object {
        private val isBootstrapping = AtomicBoolean(false)
    }

    private fun bootstrapShell() {
        scope.launch {
            _isRunning.value = true
            val sentinelFile = File(context.filesDir, "usr/.bootstrapped")
            val profileFile = File(context.filesDir, "usr/etc/profile")

            if (!sentinelFile.exists() || !profileFile.exists()) {
                if (isBootstrapping.compareAndSet(false, true)) {
                    emulator.write("\u001B[32m[*] NEX_INITIALIZE...\u001B[0m\r\n")
                    try {
                        bootstrapManager.initialize()
                        delay(200)
                        pty.write("chmod -R 755 \"${File(context.filesDir, "usr/bin").absolutePath}\"\r")
                        sentinelFile.parentFile?.mkdirs()
                        sentinelFile.createNewFile()
                    } catch (e: Exception) {
                        emulator.write("\u001B[31m[!] FAILED: ${e.message}\u001B[0m\r\n")
                    } finally { isBootstrapping.set(false) }
                }
            }

            delay(200)
            if (profileFile.exists()) {
                pty.write(". \"${profileFile.absolutePath}\"\r")
                delay(300)
                pty.write("cd \"$currentWorkingDirectory\"\r")
                delay(100)
                pty.write("printf '\\033[2J\\033[H'\r")
                delay(300)
            }
            bootstrapActive = false
            _isRunning.value = false
        }
    }

    private fun setupEnvironment() {
        listOf("usr", "usr/bin", "usr/lib", "usr/etc", "tmp", "home", "packages").forEach {
            File(context.filesDir, it).apply { if (!exists()) mkdirs() }
        }
    }

    private fun startReader() {
        readerJob?.cancel()
        readerJob = scope.launch {
            val buffer = ByteArray(4096)
            while (pty.isRunning()) {
                val count = pty.read(buffer)
                if (count > 0) {
                    val text = buffer.decodeToString(0, count)
                    emulator.write(text)
                    _output.value += text
                    updateRunningState(text)
                } else if (count < 0) break
            }
            _isRunning.value = false
        }
    }

    private fun updateRunningState(text: String) {
        if (bootstrapActive) return
        val clean = text.replace("\r", "").trim()
        // If the output ends with our hacker prompt, we are idle
        if (clean.endsWith("#") || clean.endsWith("$")) {
            Log.e("TerminalSession", "Prompt detected, hiding indicator")
            _isRunning.value = false
        }
    }

    private fun startWaiter() {
        waiterJob?.cancel()
        waiterJob = scope.launch {
            pty.waitFor()
            _isRunning.value = false
        }
    }

    fun resize(rows: Int, cols: Int) { emulator.resize(rows, cols); pty.resize(rows, cols) }
    fun terminate() { readerJob?.cancel(); waiterJob?.cancel(); pty.destroy(); _isRunning.value = false; scope.cancel() }
    fun writeToEmulator(text: String) { emulator.write(text) }

    suspend fun sendInput(input: String) { pty.write(input) }
    suspend fun sendKey(key: String) {
        when (key.uppercase()) {
            "ENTER" -> pty.write("\r")
            "CTRL+C" -> { pty.write(byteArrayOf(0x03)); _isRunning.value = false }
            "UP" -> pty.write("\u001B[A")
            "DOWN" -> pty.write("\u001B[B")
            else -> if (key.length == 1) pty.write(key)
        }
    }

    suspend fun executeCommand(command: String) {
        _isRunning.value = true
        pty.write(command)
        pty.write("\r")
    }
}
