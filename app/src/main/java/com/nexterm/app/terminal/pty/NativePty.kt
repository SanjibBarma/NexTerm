package com.nexterm.app.terminal.pty

data class PtyProcess(
    val fd: Int,
    val pid: Int
)

class NativePty {

    companion object {
        init {
            System.loadLibrary("nexterm-pty")
        }
    }

    external fun nativeCreateProcess(
        shell: String,
        args: Array<String>,
        envVars: Array<String>,
        cwd: String,
        rows: Int,
        cols: Int
    ): PtyProcess?

    external fun nativeRead(
        fd: Int,
        buffer: ByteArray,
        offset: Int,
        length: Int
    ): Int

    external fun nativeWrite(
        fd: Int,
        data: ByteArray,
        offset: Int,
        length: Int
    ): Int

    external fun nativeResize(
        fd: Int,
        rows: Int,
        cols: Int
    )

    external fun nativeClose(fd: Int)

    external fun nativeHangupProcess(pid: Int)

    external fun nativeInterruptProcess(pid: Int)

    external fun nativeIsAlive(pid: Int): Boolean

    external fun nativeWaitFor(pid: Int): Int
}