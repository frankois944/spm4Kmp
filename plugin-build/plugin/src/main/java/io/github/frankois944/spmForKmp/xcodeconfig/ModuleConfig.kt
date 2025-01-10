package io.github.frankois944.spmForKmp.xcodeconfig

import java.io.File

internal data class ModuleConfig(
    val isFramework: Boolean,
    val name: String,
    val buildDir: File,
    val definitionFile: File,
)
