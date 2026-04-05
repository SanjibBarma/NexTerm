package com.nexterm.app.terminal.pty

import java.io.IOException

class Pty(
    private var rows: Int = 24,
    private var cols: Int = 80
) {
    private val nativePty = NativePty()

    private var fd: Int = -1
    var pid: Int = -1
        private set

    fun startShell(
        shell: String = "/system/bin/sh",
        args: Array<String> = arrayOf(shell, "-"),
        environment: Map<String, String> = emptyMap(),
        workingDirectory: String
    ): Boolean {
        val envArray = buildEnvironment(environment, workingDirectory).toTypedArray()

        val process = nativePty.nativeCreateProcess(
            shell = shell,
            args = args,
            envVars = envArray,
            cwd = workingDirectory,
            rows = rows,
            cols = cols
        ) ?: return false

        fd = process.fd
        pid = process.pid
        return fd >= 0 && pid > 0
    }

    private fun buildEnvironment(
        additionalEnv: Map<String, String>,
        home: String
    ): List<String> {
        val prefix = "$home/usr"
        val env = linkedMapOf<String, String>()

        env["TERM"] = "xterm-256color"
        env["COLORTERM"] = "truecolor"
        env["HOME"] = home
        env["PWD"] = home
        env["USER"] = "shell"
        env["LOGNAME"] = "shell"
        env["SHELL"] = "/system/bin/sh"
        env["LANG"] = "en_US.UTF-8"
        env["LC_ALL"] = "en_US.UTF-8"
        env["TMPDIR"] = "$home/tmp"
        env["PREFIX"] = prefix
        env["LD_LIBRARY_PATH"] = "$prefix/lib"
        env["PATH"] = "$prefix/bin:/system/bin:/system/xbin:/vendor/bin"
        env["ANDROID_ROOT"] = System.getenv("ANDROID_ROOT") ?: "/system"
        env["ANDROID_DATA"] = System.getenv("ANDROID_DATA") ?: "/data"
        env["EXTERNAL_STORAGE"] = System.getenv("EXTERNAL_STORAGE") ?: "/sdcard"
        env["EDITOR"] = "nano"
        env["VISUAL"] = "nano"
        env["PAGER"] = "cat"
        env["PS1"] = "nexterm$ "

        additionalEnv.forEach { (key, value) ->
            env[key] = value
        }

        return env.map { "${it.key}=${it.value}" }
    }

    fun resize(rows: Int, cols: Int) {
        this.rows = rows
        this.cols = cols
        if (fd >= 0) {
            nativePty.nativeResize(fd, rows, cols)
        }
    }

    fun write(data: ByteArray) {
        if (fd < 0) return
        var offset = 0
        while (offset < data.size) {
            val written = nativePty.nativeWrite(fd, data, offset, data.size - offset)
            if (written <= 0) break
            offset += written
        }
    }

    fun write(data: String) {
        write(data.toByteArray(Charsets.UTF_8))
    }

    fun read(buffer: ByteArray): Int {
        if (fd < 0) return -1
        return nativePty.nativeRead(fd, buffer, 0, buffer.size)
    }

    fun isRunning(): Boolean {
        return pid > 0 && nativePty.nativeIsAlive(pid)
    }

    fun interrupt() {
        if (pid > 0) {
            nativePty.nativeInterruptProcess(pid)
        }
    }

    fun waitFor(): Int {
        return if (pid > 0) nativePty.nativeWaitFor(pid) else -1
    }

    fun destroy() {
        try {
            if (pid > 0) {
                nativePty.nativeHangupProcess(pid)
            }
        } catch (_: Exception) {
        }

        try {
            if (fd >= 0) {
                nativePty.nativeClose(fd)
            }
        } catch (_: IOException) {
        }

        fd = -1
        pid = -1
    }

    fun hangup() {
        destroy()
    }
}