package io.github.frankois944.spmForKmp.config

import java.io.File

internal data class ModuleConfig(
    val isFramework: Boolean,
    val name: String,
    val packageName: String,
    val buildDir: File,
    val definitionFile: File,
    val linkerOpts: List<String>,
    val compilerOpts: List<String>,
)

internal data class ModuleInfo(
    val name: String,
    val packageName: String = name,
    val linkerOpts: List<String> = emptyList(),
    val compilerOpts: List<String> = emptyList(),
)
