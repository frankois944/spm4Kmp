package io.github.frankois944.spmForKmp.config

import java.io.File

internal data class ModuleConfig(
    var isFramework: Boolean = false,
    var name: String = "",
    var packageName: String = "",
    var buildDir: File = File(""),
    var definitionFile: File = File(""),
    var linkerOpts: List<String> = emptyList(),
    var compilerOpts: List<String> = emptyList(),
    var isSwiftBridge: Boolean = false,
)
