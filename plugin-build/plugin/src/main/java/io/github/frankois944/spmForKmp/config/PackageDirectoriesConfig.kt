package io.github.frankois944.spmForKmp.config

import java.io.File

internal data class PackageDirectoriesConfig(
    val spmWorkingDir: File,
    val packageScratchDir: File,
    val sharedCacheDir: File?,
    val sharedConfigDir: File?,
    val sharedSecurityDir: File?,
    val bridgeSourceDir: File,
)
