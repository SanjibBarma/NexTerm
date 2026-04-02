package com.nexterm.app.terminal.shell

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.nexterm.app.terminal.pty.Pty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class ShellExecutor(private val context: Context) {

    private var pty: Pty? = null
    private val isRunning = AtomicBoolean(false)
    private val executor = Executors.newSingleThreadExecutor()
    private var readerThread: Thread? = null

    private val homeDir: String by lazy { context.filesDir.absolutePath }

    init {
        setupEnvironment()
    }

    private fun setupEnvironment() {
        // Create necessary directories
        listOf(
            File(homeDir, "usr/bin"),
            File(homeDir, "usr/lib"),
            File(homeDir, "usr/etc"),
            File(homeDir, "usr/share"),
            File(homeDir, "tmp"),
            File(homeDir, "home")
        ).forEach { dir ->
            if (!dir.exists()) dir.mkdirs()
        }

        // Create .bashrc / profile
        createShellProfile()

        // Create initial scripts
        createHelperScripts()
    }

    private fun createShellProfile() {
        val profile = File(homeDir, ".profile")
        if (!profile.exists()) {
            profile.writeText("""
                # NexTerm Shell Profile
                export HOME="$homeDir"
                export PATH="$homeDir/usr/bin:/system/bin:/system/xbin"
                export TERM=xterm-256color
                export PS1='\033[32mnexterm\033[0m:\033[34m\w\033[0m$ '
                
                # Aliases
                alias ll='ls -la'
                alias la='ls -a'
                alias l='ls -CF'
                alias cls='clear'
                alias ..='cd ..'
                alias ...='cd ../..'
                
                # Welcome message
                echo ""
                echo -e "\033[32m╔═══════════════════════════════════════════╗\033[0m"
                echo -e "\033[32m║\033[0m     Welcome to \033[1;36mNexTerm\033[0m Terminal          \033[32m║\033[0m"
                echo -e "\033[32m║\033[0m     Type '\033[33mhelp\033[0m' for available commands    \033[32m║\033[0m"
                echo -e "\033[32m╚═══════════════════════════════════════════╝\033[0m"
                echo ""
                
            """.trimIndent())
        }
    }

    private fun createHelperScripts() {
        // Create a simple 'neofetch' like script
        val neofetch = File(homeDir, "usr/bin/neofetch")
        neofetch.writeText("""
            #!/system/bin/sh
            echo ""
            echo -e "\033[36m   _   _          _____                   \033[0m"
            echo -e "\033[36m  | \ | |        |_   _|                  \033[0m"
            echo -e "\033[36m  |  \| | _____  __ | |  ___ _ __ _ __ ___\033[0m"
            echo -e "\033[36m  | . \` |/ _ \ \/ / | | / _ \ '__| '_ \` _ \\\\\033[0m"
            echo -e "\033[36m  | |\  |  __/>  <  | ||  __/ |  | | | | | |\033[0m"
            echo -e "\033[36m  |_| \_|\___/_/\_\ \_/ \___|_|  |_| |_| |_|\033[0m"
            echo ""
            echo -e "\033[33mOS:\033[0m Android $(getprop ro.build.version.release)"
            echo -e "\033[33mDevice:\033[0m $(getprop ro.product.model)"
            echo -e "\033[33mKernel:\033[0m $(uname -r)"
            echo -e "\033[33mShell:\033[0m /system/bin/sh"
            echo -e "\033[33mTerminal:\033[0m NexTerm v1.0.0"
            echo ""
        """.trimIndent())
        neofetch.setExecutable(true)

        // Create clear command
        val clear = File(homeDir, "usr/bin/clear")
        clear.writeText("""
            #!/system/bin/sh
            printf '\033[2J\033[H'
        """.trimIndent())
        clear.setExecutable(true)

        // Create help command
        val help = File(homeDir, "usr/bin/help")
        help.writeText("""
            #!/system/bin/sh
            echo ""
            echo -e "\033[1;32mNexTerm Help\033[0m"
            echo -e "\033[90m─────────────────────────────────────────\033[0m"
            echo ""
            echo -e "\033[33mFile Operations:\033[0m"
            echo "  ls, cd, pwd, cat, mkdir, rm, touch, cp, mv"
            echo ""
            echo -e "\033[33mPackage Management:\033[0m"
            echo "  pkg install <package>  - Install a package"
            echo "  pkg remove <package>   - Remove a package"
            echo "  pkg list               - List installed packages"
            echo "  pkg search <query>     - Search packages"
            echo ""
            echo -e "\033[33mDevelopment:\033[0m"
            echo "  python, node, npm, pip, git, vim, nano"
            echo ""
            echo -e "\033[33mServers:\033[0m"
            echo "  nginx, apache2, mysql, redis-server, sshd"
            echo ""
            echo -e "\033[33mNetwork:\033[0m"
            echo "  ping, curl, wget, ssh, nmap, netstat"
            echo ""
            echo -e "\033[33mSystem:\033[0m"
            echo "  whoami, uname, date, env, clear, exit"
            echo ""
            echo -e "\033[33mOther:\033[0m"
            echo "  neofetch  - System information"
            echo "  htop      - Process viewer"
            echo ""
        """.trimIndent())
        help.setExecutable(true)
    }

    fun startShell(
        workingDirectory: String = homeDir,
        additionalEnv: Map<String, String> = emptyMap(),
        rows: Int = 24,
        cols: Int = 80
    ): Flow<ShellOutput> = callbackFlow {
        try {
            isRunning.set(true)

            pty = Pty(rows, cols)

            val shellStarted = pty?.startShell(
                shell = "/system/bin/sh",
                args = arrayOf("-l"), // Login shell
                environment = additionalEnv,
                workingDirectory = workingDirectory
            ) ?: false

            if (!shellStarted) {
                trySend(ShellOutput.Error("Failed to start shell"))
                close()
                return@callbackFlow
            }

            trySend(ShellOutput.Started)

            // Send initial commands to set up environment
            pty?.write("export PS1='\\033[32mnexterm\\033[0m:\\033[34m\\w\\033[0m\$ '\n")
            pty?.write("export TERM=xterm-256color\n")
            pty?.write("export PATH=\"$homeDir/usr/bin:\$PATH\"\n")
            pty?.write("cd $workingDirectory\n")
            pty?.write("clear\n")

            // Show welcome message
            pty?.write("echo ''\n")
            pty?.write("echo -e '\\033[32m╔═══════════════════════════════════════════╗\\033[0m'\n")
            pty?.write("echo -e '\\033[32m║\\033[0m     Welcome to \\033[1;36mNexTerm\\033[0m Terminal          \\033[32m║\\033[0m'\n")
            pty?.write("echo -e '\\033[32m║\\033[0m     Type \\033[33mhelp\\033[0m for available commands    \\033[32m║\\033[0m'\n")
            pty?.write("echo -e '\\033[32m╚═══════════════════════════════════════════╝\\033[0m'\n")
            pty?.write("echo ''\n")

            // Read output in a separate thread
            readerThread = Thread {
                val buffer = ByteArray(8192)
                try {
                    while (isRunning.get() && pty?.isRunning() == true) {
                        val bytesRead = pty?.read(buffer) ?: -1
                        if (bytesRead > 0) {
                            val output = String(buffer, 0, bytesRead)
                            // Filter out the TTY warning messages
                            val filteredOutput = filterOutput(output)
                            if (filteredOutput.isNotEmpty()) {
                                trySend(ShellOutput.Data(filteredOutput))
                            }
                        } else if (bytesRead == -1) {
                            break
                        }
                        Thread.sleep(10)
                    }
                } catch (e: Exception) {
                    if (isRunning.get()) {
                        trySend(ShellOutput.Error(e.message ?: "Read error"))
                    }
                }

                val exitCode = pty?.waitFor() ?: -1
                trySend(ShellOutput.Exited(exitCode))
            }
            readerThread?.start()

            awaitClose {
                terminate()
            }

        } catch (e: Exception) {
            trySend(ShellOutput.Error(e.message ?: "Unknown error"))
            close()
        }
    }.flowOn(Dispatchers.IO)

    private fun filterOutput(output: String): String {
        // Filter out common warning messages that appear at startup
        val linesToFilter = listOf(
            "can't find tty fd",
            "No such device or address",
            "won't have full job control",
            "warning:",
            "cannot set terminal process group",
            "no job control in this shell"
        )

        return output.lines()
            .filterNot { line ->
                linesToFilter.any { filter ->
                    line.contains(filter, ignoreCase = true)
                }
            }
            .joinToString("\n")
    }

    suspend fun executeCommand(command: String): Flow<ShellOutput> = callbackFlow {
        try {
            val processBuilder = ProcessBuilder("/system/bin/sh", "-c", command).apply {
                directory(File(homeDir))
                environment().apply {
                    put("HOME", homeDir)
                    put("PATH", "$homeDir/usr/bin:/system/bin:/system/xbin")
                    put("TERM", "xterm-256color")
                }
                redirectErrorStream(true)
            }

            val startTime = System.currentTimeMillis()
            val process = processBuilder.start()

            val reader = process.inputStream.bufferedReader()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                trySend(ShellOutput.Data(line!! + "\n"))
            }

            val exitCode = process.waitFor()
            val executionTime = System.currentTimeMillis() - startTime

            trySend(ShellOutput.CommandCompleted(exitCode, executionTime))
            close()

        } catch (e: Exception) {
            trySend(ShellOutput.Error(e.message ?: "Unknown error"))
            close()
        }
    }.flowOn(Dispatchers.IO)

    suspend fun sendInput(input: String) = withContext(Dispatchers.IO) {
        try {
            pty?.write(input)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun sendSpecialKey(key: SpecialKey) = withContext(Dispatchers.IO) {
        val sequence = when (key) {
            SpecialKey.CTRL_C -> "\u0003"
            SpecialKey.CTRL_D -> "\u0004"
            SpecialKey.CTRL_Z -> "\u001A"
            SpecialKey.CTRL_L -> "\u000C"
            SpecialKey.CTRL_A -> "\u0001"
            SpecialKey.CTRL_E -> "\u0005"
            SpecialKey.CTRL_U -> "\u0015"
            SpecialKey.CTRL_K -> "\u000B"
            SpecialKey.CTRL_W -> "\u0017"
            SpecialKey.TAB -> "\t"
            SpecialKey.ENTER -> "\n"
            SpecialKey.BACKSPACE -> "\u007F"
            SpecialKey.ESCAPE -> "\u001B"
            SpecialKey.UP -> "\u001B[A"
            SpecialKey.DOWN -> "\u001B[B"
            SpecialKey.RIGHT -> "\u001B[C"
            SpecialKey.LEFT -> "\u001B[D"
            SpecialKey.HOME -> "\u001B[H"
            SpecialKey.END -> "\u001B[F"
            SpecialKey.PAGE_UP -> "\u001B[5~"
            SpecialKey.PAGE_DOWN -> "\u001B[6~"
            SpecialKey.DELETE -> "\u001B[3~"
            SpecialKey.INSERT -> "\u001B[2~"
            SpecialKey.F1 -> "\u001BOP"
            SpecialKey.F2 -> "\u001BOQ"
            SpecialKey.F3 -> "\u001BOR"
            SpecialKey.F4 -> "\u001BOS"
            SpecialKey.F5 -> "\u001B[15~"
            SpecialKey.F6 -> "\u001B[17~"
            SpecialKey.F7 -> "\u001B[18~"
            SpecialKey.F8 -> "\u001B[19~"
            SpecialKey.F9 -> "\u001B[20~"
            SpecialKey.F10 -> "\u001B[21~"
            SpecialKey.F11 -> "\u001B[23~"
            SpecialKey.F12 -> "\u001B[24~"
        }
        pty?.write(sequence)
    }

    fun resize(rows: Int, cols: Int) {
        pty?.resize(rows, cols)
    }

    fun terminate() {
        isRunning.set(false)
        readerThread?.interrupt()
        pty?.destroy()
        pty = null
    }

    fun isRunning(): Boolean = isRunning.get() && (pty?.isRunning() == true)

    companion object {
        const val SIGINT = 2
        const val SIGQUIT = 3
        const val SIGTSTP = 20
    }
}

enum class SpecialKey {
    CTRL_C, CTRL_D, CTRL_Z, CTRL_L, CTRL_A, CTRL_E, CTRL_U, CTRL_K, CTRL_W,
    TAB, ENTER, BACKSPACE, ESCAPE,
    UP, DOWN, RIGHT, LEFT, HOME, END, PAGE_UP, PAGE_DOWN, DELETE, INSERT,
    F1, F2, F3, F4, F5, F6, F7, F8, F9, F10, F11, F12
}

sealed class ShellOutput {
    object Started : ShellOutput()
    data class Data(val text: String) : ShellOutput()
    data class Error(val message: String) : ShellOutput()
    data class CommandCompleted(val exitCode: Int, val executionTimeMs: Long) : ShellOutput()
    data class Exited(val exitCode: Int) : ShellOutput()
}