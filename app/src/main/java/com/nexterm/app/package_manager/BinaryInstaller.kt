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

    fun installOrUpdateIfNeeded(manifest: BinaryManifest): Result<Boolean> {
        return runCatching {
            ensureBaseDirs()

            val installedVersion = versionFile.takeIf { it.exists() }?.readText()?.trim()?.toIntOrNull()
            val installedAbi = abiFile.takeIf { it.exists() }?.readText()?.trim()

            val needsInstall = installedVersion != manifest.version || installedAbi != manifest.abi

            if (!needsInstall) {
                return@runCatching false
            }

            installManifest(manifest)

            versionFile.writeText(manifest.version.toString())
            abiFile.writeText(manifest.abi)

            true
        }
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