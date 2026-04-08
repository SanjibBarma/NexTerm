package com.nexterm.app.package_manager

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

class BinaryInstaller(private val context: Context) {

    private val root = context.filesDir
    private val markerDir = File(root, ".bootstrap")
    private val versionFile = File(markerDir, "binary_version.txt")
    private val abiFile = File(markerDir, "binary_abi.txt")

    fun installTermuxBootstrap(info: TermuxBootstrapInfo): Result<Boolean> {
        return runCatching {
            ensureBaseDirs()
            val usrDir = File(root, "usr")
            if (File(usrDir, "bin/bash").exists() || File(usrDir, "bin/sh").exists()) return@runCatching false

            // Mirror URLs for redundancy
            val mirrors = listOf(
                info.bootstrapUrl,
                "https://mirror.n0p.me/termux/termux-packages-24/bootstrap-${info.arch}.zip",
                "https://mirror.termux.dev/termux-packages-24/bootstrap-${info.arch}.zip"
            )

            var lastException: Exception? = null
            for (mirror in mirrors) {
                try {
                    downloadAndExtractZip(mirror, root)
                    return@runCatching true // Success!
                } catch (e: Exception) {
                    lastException = e
                    continue // Try next mirror
                }
            }
            throw lastException ?: Exception("Bootstrap failed across all mirrors.")
        }
    }

    private fun downloadAndExtractZip(urlString: String, targetDir: File) {
        val url = java.net.URL(urlString)
        val connection = url.openConnection() as java.net.HttpURLConnection
        
        // Advanced Connection Settings
        connection.connectTimeout = 30000 // 30 seconds
        connection.readTimeout = 60000    // 1 minute
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android 14; Mobile; rv:124.0) Gecko/124.0 Firefox/124.0")
        connection.setRequestProperty("Accept", "*/*")
        connection.setRequestProperty("Connection", "keep-alive")
        
        connection.connect()
        if (connection.responseCode != java.net.HttpURLConnection.HTTP_OK) {
            throw Exception("Mirror $urlString rejected: ${connection.responseCode}")
        }

        java.util.zip.ZipInputStream(connection.inputStream).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val newFile = File(targetDir, entry.name)
                if (entry.isDirectory) {
                    newFile.mkdirs()
                } else {
                    newFile.parentFile?.mkdirs()
                    java.io.FileOutputStream(newFile).use { fos ->
                        zis.copyTo(fos)
                    }
                    if (newFile.absolutePath.contains("/bin/") || newFile.absolutePath.contains("/lib/")) {
                        newFile.setExecutable(true, false)
                        newFile.setReadable(true, false)
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        connection.disconnect()
    }

    private fun ensureBaseDirs() {
        listOf(
            markerDir,
            File(root, "usr"),
            File(root, "usr/bin"),
            File(root, "usr/lib"),
            File(root, "usr/etc"),
            File(root, "usr/share"),
            File(root, "tmp"),
            File(root, "packages")
        ).forEach {
            if (!it.exists()) it.mkdirs()
        }
    }

    private fun installManifest(manifest: BinaryManifest) {
        manifest.files.forEach { asset ->
            val targetDirectory = File(root, asset.targetDir)
            if (!targetDirectory.exists()) targetDirectory.mkdirs()

            val fileName = asset.path.substringAfterLast('/')
            val outFile = File(targetDirectory, fileName)

            extractAsset(asset.path, outFile)

            if (asset.checksum != null) {
                val actual = sha256(outFile)
                if (!actual.equals(asset.checksum, ignoreCase = true)) {
                    outFile.delete()
                    error("Checksum mismatch for ${asset.path}")
                }
            }

            if (asset.executable) {
                outFile.setExecutable(true, false)
                outFile.setReadable(true, false)
                outFile.setWritable(true, true)
            }
        }
    }

    private fun extractAsset(assetPath: String, outFile: File) {
        context.assets.open(assetPath).use { input ->
            FileOutputStream(outFile).use { output ->
                input.copyTo(output)
            }
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}