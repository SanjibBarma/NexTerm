package com.nexterm.app.package_manager

import android.content.Context
import java.io.File

class BinaryCommandResolver(private val context: Context) {

    private val root = context.filesDir
    private val binDir = File(root, "usr/bin")

    fun hasRealCommand(name: String): Boolean {
        val file = File(binDir, name)
        return file.exists() && file.canExecute()
    }

    fun resolveCommandPath(name: String): String? {
        val file = File(binDir, name)
        return if (file.exists() && file.canExecute()) file.absolutePath else null
    }

    fun symlinkOrCopyFallbacks() {
        val busybox = File(binDir, "busybox")
        if (!busybox.exists() || !busybox.canExecute()) return

        val safeUtilities = listOf(
            "ls", "cat", "cp", "mv", "rm", "touch", "mkdir", "pwd",
            "echo", "uname", "date", "env", "find", "head", "tail",
            "wc", "cut", "sort", "uniq", "tr", "chmod", "stat"
        )

        safeUtilities.forEach { cmd ->
            val target = File(binDir, cmd)
            if (!target.exists()) {
                runCatching {
                    target.writeText(
                        """
                        #!/system/bin/sh
                        exec "${busybox.absolutePath}" $cmd "${'$'}@"
                        """.trimIndent()
                    )
                    target.setExecutable(true)
                }
            }
        }
    }
}