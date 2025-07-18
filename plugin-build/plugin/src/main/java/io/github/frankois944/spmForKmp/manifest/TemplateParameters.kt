package io.github.frankois944.spmForKmp.manifest

import io.github.frankois944.spmForKmp.config.ModuleConfig
import io.github.frankois944.spmForKmp.definition.SwiftDependency
import io.github.frankois944.spmForKmp.definition.exported.ExportedPackage
import io.github.frankois944.spmForKmp.definition.packageSetting.BridgeSettings
import java.nio.file.Path

internal data class TemplateParameters(
    val forExportedPackage: Boolean,
    val dependencies: List<SwiftDependency>,
    val generatedPackageDirectory: Path,
    val productName: String,
    val minIos: String,
    val minMacos: String,
    val minTvos: String,
    val minWatchos: String,
    val toolsVersion: String,
    val targetSettings: BridgeSettings?,
    val exportedPackage: ExportedPackage?,
    val onlyDeps: List<ModuleConfig> = emptyList(),
)
