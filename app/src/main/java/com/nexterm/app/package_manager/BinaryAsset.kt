package com.nexterm.app.package_manager

data class BinaryAsset(
    val path: String,
    val targetDir: String,
    val executable: Boolean = true,
    val checksum: String? = null
)

data class BinaryManifest(
    val version: Int,
    val abi: String,
    val files: List<BinaryAsset>
)