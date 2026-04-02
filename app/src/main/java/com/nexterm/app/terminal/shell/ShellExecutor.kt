package com.nexterm.app.terminal.shell

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File

class ShellExecutor(private val context: Context) {

    private val homeDir: String by lazy { context.filesDir.absolutePath }

    init {
        setupEnvironment()
    }

    private fun setupEnvironment() {
        listOf(
            File(homeDir),
            File(homeDir, "usr"),
            File(homeDir, "usr/bin"),
            File(homeDir, "usr/lib"),
            File(homeDir, "usr/etc"),
            File(homeDir, "usr/share"),
            File(homeDir, "tmp"),
            File(homeDir, "home")
        ).forEach { dir ->
            if (!dir.exists()) dir.mkdirs()
        }

        createHelperScripts()
    }

    private fun createHelperScripts() {
        val clear = File(homeDir, "usr/bin/clear")
        if (!clear.exists()) {
            clear.writeText(
                """
                #!/system/bin/sh
                printf '\033[2J\033[H'
                """.trimIndent()
            )
            clear.setExecutable(true)
        }

        val help = File(homeDir, "usr/bin/help")
        if (!help.exists()) {
            help.writeText(
                """
                #!/system/bin/sh
                echo ""
                echo "NexTerm Help"
                echo "-----------------------------"
                echo "File Operations:"
                echo "  ls, cd, pwd, cat, mkdir, rm, touch, cp, mv"
                echo ""
                echo "System:"
                echo "  clear, exit, env, uname, date"
                echo ""
                echo "Extras:"
                echo "  help, neofetch"
                echo ""
                """.trimIndent()
            )
            help.setExecutable(true)
        }

        val neofetch = File(homeDir, "usr/bin/neofetch")
        if (!neofetch.exists()) {
            neofetch.writeText(
                """
                #!/system/bin/sh
                echo ""
                echo "NexTerm"
                echo "OS: Android $(getprop ro.build.version.release)"
                echo "Device: $(getprop ro.product.model)"
                echo "Kernel: $(uname -r)"
                echo "Shell: /system/bin/sh"
                echo ""
                """.trimIndent()
            )
            neofetch.setExecutable(true)
        }
    }

    fun getHomeDirectory(): String = homeDir

    fun executeCommand(
        command: String,
        workingDirectory: String = homeDir
    ): Flow<ShellOutput> = flow {
        try {
            emit(ShellOutput.Started)

            val dir = File(workingDirectory).takeIf { it.exists() && it.isDirectory } ?: File(homeDir)

            val processBuilder = ProcessBuilder("/system/bin/sh", "-c", command).apply {
                directory(dir)
                environment().apply {
                    put("HOME", homeDir)
                    put("PATH", "$homeDir/usr/bin:/system/bin:/system/xbin:/vendor/bin")
                    put("TERM", "xterm-256color")
                    put("PWD", dir.absolutePath)
                    put("TMPDIR", "$homeDir/tmp")
                }
                redirectErrorStream(true)
            }

            val process = processBuilder.start()

            val output = buildString {
                process.inputStream.bufferedReader().useLines { lines ->
                    lines.forEach { line ->
                        append(line)
                        append("\n")
                    }
                }
            }

            if (output.isNotEmpty()) {
                emit(ShellOutput.Data(output))
            }

            val exitCode = process.waitFor()
            emit(ShellOutput.Completed(exitCode))
        } catch (e: Exception) {
            emit(ShellOutput.Error(e.message ?: "Unknown error"))
        }
    }.flowOn(Dispatchers.IO)
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
    data class Completed(val exitCode: Int) : ShellOutput()
}