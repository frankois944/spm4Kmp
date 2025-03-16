package io.github.frankois944.spmForKmp.manifest

import io.github.frankois944.spmForKmp.definition.packageSetting.BridgeSettings
import io.github.frankois944.spmForKmp.definition.packageSetting.cSetting.CSettingConfig
import io.github.frankois944.spmForKmp.definition.packageSetting.cxxSetting.CxxSettingConfig
import io.github.frankois944.spmForKmp.definition.packageSetting.linkerSetting.LinkerSettingConfig
import io.github.frankois944.spmForKmp.definition.packageSetting.swiftSetting.SwiftSettingConfig
import java.nio.file.Path
import kotlin.io.path.relativeToOrSelf

internal fun getTargetSettings(
    settings: BridgeSettings?,
    swiftBuildDir: Path,
): String =
    buildList {
        settings?.run {
            val cSettings = getCSettings(cSettings, swiftBuildDir)
            if (cSettings.isNotEmpty()) {
                add("cSettings: [$cSettings]")
            }
            val cxxSettings = getCXXSettings(cxxSettings, swiftBuildDir)
            if (cxxSettings.isNotEmpty()) {
                add("cxxSettings: [$cxxSettings]")
            }
            val swiftSettings = getSwiftSettings(swiftSettings)
            if (swiftSettings.isNotEmpty()) {
                add("swiftSettings: [$swiftSettings]")
            }
            val linkerSettings = getLinkerSettings(linkerSettings)
            if (linkerSettings.isNotEmpty()) {
                add("linkerSettings: [$linkerSettings]")
            }
        }
    }.joinToString(",\n")

private fun getCSettings(
    settings: CSettingConfig,
    swiftBuildDir: Path,
): String =
    buildList {
        settings.defines.forEach {
            add(".define(\"${it.first}\", to: \"${it.second}\")")
        }
        settings.headerSearchPath.forEach {
            val path = Path.of(it).relativeToOrSelf(swiftBuildDir)
            add(".headerSearchPath(\"$path\")")
        }
        if (settings.unsafeFlags.isNotEmpty()) {
            add(".unsafeFlags([${settings.unsafeFlags.joinToString(",") { "\"$it\"" }}])")
        }
    }.joinToString(",\n")

private fun getCXXSettings(
    settings: CxxSettingConfig,
    swiftBuildDir: Path,
): String =
    buildList {
        settings.defines.forEach {
            add(".define(\"${it.first}\", to: \"${it.second}\")")
        }
        settings.headerSearchPath.forEach {
            val path = Path.of(it).relativeToOrSelf(swiftBuildDir)
            add(".headerSearchPath(\"$path\")")
        }
        if (settings.unsafeFlags.isNotEmpty()) {
            add(".unsafeFlags([${settings.unsafeFlags.joinToString(",") { "\"$it\"" }}])")
        }
    }.joinToString(",\n")

private fun getSwiftSettings(settings: SwiftSettingConfig): String =
    buildList {
        settings.defines.forEach {
            add(".define(\"${it}\")")
        }
        if (settings.unsafeFlags.isNotEmpty()) {
            add(".unsafeFlags([${settings.unsafeFlags.joinToString(",") { "\"$it\"" }}])")
        }
        settings.enableExperimentalFeature?.let {
            add(".enableExperimentalFeature(\"${it}\")")
        }
        settings.enableUpcomingFeature?.let {
            add(".enableUpcomingFeature(\"${it}\")")
        }
        settings.interoperabilityMode?.let {
            add(".interoperabilityMode(.$it)")
        }
        settings.swiftLanguageMode?.let {
            add(".swiftLanguageMode(.version(\"$it\"))")
        }
    }.joinToString(",\n")

private fun getLinkerSettings(settings: LinkerSettingConfig): String =
    buildList {
        settings.linkedFramework.forEach {
            add(".linkedFramework(\"${it}\")")
        }
        settings.linkedLibrary.forEach {
            add(".linkedLibrary(\"${it}\")")
        }
        if (settings.unsafeFlags.isNotEmpty()) {
            add(".unsafeFlags([${settings.unsafeFlags.joinToString(",") { "\"$it\"" }}])")
        }
    }.joinToString(",\n")
