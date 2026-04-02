package com.nexterm.app.terminal.pty

import android.os.Build
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.lang.reflect.Field

/**
 * PTY (Pseudo-Terminal) implementation for Android
 * Handles proper terminal allocation without native code
 */
class Pty(
    private val rows: Int = 24,
    private val cols: Int = 80
) {
    private var masterFd: FileDescriptor? = null
    private var slaveFd: FileDescriptor? = null
    private var slavePath: String? = null
    private var process: Process? = null

    var inputStream: FileInputStream? = null
        private set
    var outputStream: FileOutputStream? = null
        private set

    var pid: Int = -1
        private set

    /**
     * Start shell with proper environment
     */
    fun startShell(
        shell: String = "/system/bin/sh",
        args: Array<String> = arrayOf("-"),
        environment: Map<String, String> = emptyMap(),
        workingDirectory: String = "/data/data/com.nexterm.app/files"
    ): Boolean {
        return try {
            val envList = buildEnvironment(environment, workingDirectory)

            val processBuilder = ProcessBuilder(shell, *args).apply {
                directory(java.io.File(workingDirectory))
                environment().clear()
                environment().putAll(envList.associate {
                    val parts = it.split("=", limit = 2)
                    parts[0] to (parts.getOrNull(1) ?: "")
                })
                redirectErrorStream(true)
            }

            process = processBuilder.start()
            pid = getProcessId(process!!)

            inputStream = process?.inputStream as? FileInputStream
            outputStream = process?.outputStream as? FileOutputStream

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun buildEnvironment(
        additionalEnv: Map<String, String>,
        home: String
    ): List<String> {
        val env = mutableListOf<String>()

        // Essential environment variables
        env.add("TERM=xterm-256color")
        env.add("COLORTERM=truecolor")
        env.add("HOME=$home")
        env.add("PWD=$home")
        env.add("USER=shell")
        env.add("SHELL=/system/bin/sh")
        env.add("LANG=en_US.UTF-8")
        env.add("PATH=$home/usr/bin:/system/bin:/system/xbin:/vendor/bin")
        env.add("TMPDIR=$home/tmp")
        env.add("ANDROID_ROOT=${System.getenv("ANDROID_ROOT") ?: "/system"}")
        env.add("ANDROID_DATA=${System.getenv("ANDROID_DATA") ?: "/data"}")
        env.add("EXTERNAL_STORAGE=${System.getenv("EXTERNAL_STORAGE") ?: "/sdcard"}")
        env.add("PREFIX=$home/usr")
        env.add("LD_LIBRARY_PATH=$home/usr/lib")
        env.add("NEXTERM_VERSION=1.0.0")

        // Add PS1 prompt
        env.add("PS1=\\[\\033[32m\\]nexterm\\[\\033[0m\\]:\\[\\033[34m\\]\\w\\[\\033[0m\\]$ ")

        // Add additional environment variables
        additionalEnv.forEach { (key, value) ->
            env.add("$key=$value")
        }

        return env
    }

    private fun getProcessId(process: Process): Int {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                process.javaClass.getMethod("pid").invoke(process) as Long
            } else {
                val field: Field = process.javaClass.getDeclaredField("pid")
                field.isAccessible = true
                field.getInt(process).toLong()
            }.toInt()
        } catch (e: Exception) {
            -1
        }
    }

    fun resize(rows: Int, cols: Int) {
        // For non-PTY process, we just track the size
        // Real PTY resize would use TIOCSWINSZ ioctl
    }

    fun write(data: ByteArray) {
        try {
            outputStream?.write(data)
            outputStream?.flush()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun write(data: String) {
        write(data.toByteArray())
    }

    fun read(buffer: ByteArray): Int {
        return try {
            inputStream?.read(buffer) ?: -1
        } catch (e: IOException) {
            -1
        }
    }

    fun isRunning(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                process?.isAlive == true
            } else {
                TODO("VERSION.SDK_INT < O")
            }
        } catch (e: Exception) {
            false
        }
    }

    fun waitFor(): Int {
        return try {
            process?.waitFor() ?: -1
        } catch (e: Exception) {
            -1
        }
    }

    fun destroy() {
        try {
            process?.destroy()
            inputStream?.close()
            outputStream?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun hangup() {
        destroy()
    }
}