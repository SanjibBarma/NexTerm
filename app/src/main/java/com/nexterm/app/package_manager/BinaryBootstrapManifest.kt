package com.nexterm.app.package_manager

import android.os.Build

object BinaryBootstrapManifest {

    fun currentAbi(): String {
        val abi = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"
        return when {
            abi.contains("arm64") -> "aarch64"
            abi.contains("armeabi") -> "arm"
            abi.contains("x86_64") -> "x86_64"
            abi.contains("x86") -> "i686"
            else -> "aarch64"
        }
    }

    /**
     * Termux Official Bootstrap Manifest.
     * These URLs point to the official Termux bootstrap packages.
     */
    fun termuxBootstrapForCurrentAbi(): TermuxBootstrapInfo {
        val arch = currentAbi()
        // Using a reliable Termux bootstrap mirror
        val baseUrl = "https://github.com/termux/termux-packages/releases/latest/download"
        
        return TermuxBootstrapInfo(
            version = 1,
            arch = arch,
            bootstrapUrl = "$baseUrl/bootstrap-$arch.zip"
        )
    }

    /**
     * Keep existing manifest for compatibility but we will primarily use TermuxBootstrapInfo
     */
    fun manifestForCurrentAbi(): BinaryManifest {
        val abi = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"
        return BinaryManifest(
            version = 1,
            abi = abi,
            files = emptyList() // We will use remote bootstrap instead
        )
    }
}

data class TermuxBootstrapInfo(
    val version: Int,
    val arch: String,
    val bootstrapUrl: String
)
