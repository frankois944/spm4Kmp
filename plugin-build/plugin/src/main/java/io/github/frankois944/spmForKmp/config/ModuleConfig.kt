package io.github.frankois944.spmForKmp.config

import io.github.frankois944.spmForKmp.definition.SwiftDependency
import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path

internal data class ModuleConfig(
    var isFramework: Boolean = false,
    var name: String = "",
    var spmPackageName: String? = null,
    var packageName: String = "",
    var buildDir: Path = Path(""),
    var definitionFile: File = File(""),
    var linkerOpts: List<String> = emptyList(),
    var compilerOpts: List<String> = emptyList(),
    var swiftDependency: SwiftDependency? = null,
    var isCLang: Boolean = false,
)

internal fun List<ModuleConfig>.containsPackage(name: String): Boolean =
    this
        .map {
            it.spmPackageName ?: it.packageName
        }.contains(name)

internal fun List<ModuleConfig>.containsProduct(name: String) = this.map { it.name }.contains(name)
