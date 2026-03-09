package io.github.frankois944.spmForKmp.config

import io.github.frankois944.spmForKmp.definition.SwiftDependency
import java.io.File
import java.nio.file.Path

internal data class ModuleConfig(
    var isFramework: Boolean = false,
    var name: String = "",
    var alias: String? = null,
    var spmPackageName: String? = null,
    var packageName: String = "",
    var buildDir: Path = Path.of(""),
    var definitionFile: File = File(""),
    var linkerOpts: List<String> = emptyList(),
    var compilerOpts: List<String> = emptyList(),
    var swiftDependency: SwiftDependency? = null,
    var isCLang: Boolean = false,
    var customSearchHeaderPath: MutableList<File> = mutableListOf(),
)

internal fun List<ModuleConfig>.containsPackage(name: String): Boolean =
    this
        .map {
            it.spmPackageName ?: it.packageName
        }.contains(name)

internal fun List<ModuleConfig>.containsProduct(name: String) =
    this
        .map {
            it.alias ?: it.name
        }.contains(name)
