package io.github.frankois944.spmForKmp.config

import java.io.File

internal data class ModuleConfig(
    val isFramework: Boolean,
    val name: String,
    val packageName: String,
    val buildDir: File,
    val definitionFile: File,
)

internal data class ModuleInfo(
    val name: String,
    val packageName: String = name,
)
