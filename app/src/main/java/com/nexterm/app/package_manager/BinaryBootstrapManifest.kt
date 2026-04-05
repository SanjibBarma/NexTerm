package com.nexterm.app.package_manager

import android.os.Build

object BinaryBootstrapManifest {

    fun currentAbi(): String {
        return Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"
    }

    /**
     * Safe built-in bootstrap manifest.
     * Add only benign utility binaries in assets/bootstrap/{abi}/...
     */
    fun manifestForCurrentAbi(): BinaryManifest {
        val abi = currentAbi()

        return BinaryManifest(
            version = 1,
            abi = abi,
            files = listOf(
                BinaryAsset(
                    path = "bootstrap/$abi/bin/busybox",
                    targetDir = "usr/bin",
                    executable = true
                ),
                BinaryAsset(
                    path = "bootstrap/$abi/bin/toybox",
                    targetDir = "usr/bin",
                    executable = true
                )
            )
        )
    }
}