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
)
{
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
        val binPath = File(context.filesDir, "usr/bin").absolutePath
        val ok = pty.startShell(
            shell = "/system/bin/sh",
            args = arrayOf("/system/bin/sh", "-i"),
            environment = mapOf(
                "HOME" to context.filesDir.absolutePath,
                "PWD" to currentWorkingDirectory,
                "PATH" to "$binPath:/system/bin:/system/xbin:/vendor/bin",
                "LD_LIBRARY_PATH" to File(context.filesDir, "usr/lib").absolutePath,
                "TERM" to "xterm-256color"
            ),
            workingDirectory = currentWorkingDirectory
        )
        if (!ok) {
            emulator.write("\u001B[31mFailed to start shell\u001B[0m\r\n")
            return
        }
        // Write banner before clearing screen
        emulator.write("\u001B[32m╔═════════════════════════════════╗\u001B[0m\r\n")
        emulator.write("\u001B[32m║      NexTerm Terminal Emulator       ║\u001B[0m\r\n")
        emulator.write("\u001B[32m╚═════════════════════════════════╝\u001B[0m\r\n")
        emulator.write("\u001B[33m[*] Initializing shell environment...\u001B[0m\r\n")
        startReader()
        startWaiter()
        bootstrapShell()
    }

    companion object {
        private val isBootstrapping = AtomicBoolean(false)
    }

    private fun bootstrapShell() {
        scope.launch {
            Log.e("TerminalSession", "bootstrapShell() started")
            _isRunning.value = true
            val sentinelFile = File(context.filesDir, "usr/.bootstrapped")
            val profileFile = File(context.filesDir, "usr/etc/profile")

            if (!sentinelFile.exists() || !profileFile.exists()) {
                if (isBootstrapping.compareAndSet(false, true)) {
                    Log.e("TerminalSession", "Running bootstrap initialization")
                    emulator.write("\u001B[32m[*] NEX_INITIALIZE...\u001B[0m\r\n")
                    try {
                        bootstrapManager.initialize()
                        delay(200)
                        Log.e("TerminalSession", "Sending chmod command")
                        pty.write("chmod -R 755 \"${File(context.filesDir, "usr/bin").absolutePath}\"\r")
                        delay(500)
                        sentinelFile.parentFile?.mkdirs()
                        sentinelFile.createNewFile()
                    } catch (e: Exception) {
                        Log.e("TerminalSession", "Bootstrap failed: ${e.message}")
                        emulator.write("\u001B[31m[!] FAILED: ${e.message}\u001B[0m\r\n")
                    } finally { isBootstrapping.set(false) }
                }
            }

            delay(300)
            Log.e("TerminalSession", "Setting permissions on usr/bin")
            pty.write("chmod -R 755 \"${File(context.filesDir, "usr/bin").absolutePath}\"\r")
            delay(500)

            if (profileFile.exists()) {
                Log.e("TerminalSession", "Sourcing profile")
                pty.write(". \"${profileFile.absolutePath}\"\r")
                delay(500)
            }

            Log.e("TerminalSession", "Changing directory to $currentWorkingDirectory")
            pty.write("cd \"$currentWorkingDirectory\"\r")
            delay(500)

            Log.e("TerminalSession", "Sending ready message")
            emulator.write("\u001B[32m[✓] Terminal ready!\u001B[0m\r\n")
            pty.write("echo '[NexTerm] Shell prompt ready'\r")
            delay(300)

            Log.e("TerminalSession", "Bootstrap complete")
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
            Log.e("TerminalSession", "Reader thread started")
            var consecutiveZeroReads = 0
            while (true) {
                try {
                    val count = pty.read(buffer)
                    if (count > 0) {
                        consecutiveZeroReads = 0
                        val text = buffer.decodeToString(0, count)
                        Log.e("TerminalSession", "Read $count bytes: $text")
                        emulator.write(text)
                        _output.value += text
                        updateRunningState(text)
                    } else if (count < 0) {
                        Log.e("TerminalSession", "Read returned -1, exiting reader")
                        break
                    } else {
                        // count == 0
                        consecutiveZeroReads++
                        if (consecutiveZeroReads > 100 && !pty.isRunning()) {
                            Log.e("TerminalSession", "PTY not running and no data, exiting reader")
                            break
                        }
                        delay(10)
                    }
                } catch (e: Exception) {
                    Log.e("TerminalSession", "Exception in reader: ${e.message}")
                    break
                }
            }
            Log.e("TerminalSession", "Reader thread exiting")
            _isRunning.value = false
        }
    }

    private fun updateRunningState(text: String) {
        if (bootstrapActive) return
        // Remove ANSI escape codes before checking for the prompt
        val clean = text.replace(Regex("\u001B\\[[;\\d]*[mK]"), "").replace("\r", "").trim()

        // Check if the output ends with a prompt character
        if (clean.endsWith("#") || clean.endsWith("$") || clean.contains("anon@nexterm")) {
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
