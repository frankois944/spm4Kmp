package io.github.frankois944.spmForKmp.manifest

import io.github.frankois944.spmForKmp.definition.packageSetting.TargetSettings
import io.github.frankois944.spmForKmp.definition.packageSetting.cSetting.CSettingConfig
import io.github.frankois944.spmForKmp.definition.packageSetting.cxxSetting.CxxSettingConfig
import io.github.frankois944.spmForKmp.definition.packageSetting.linkerSetting.LinkerSettingConfig
import io.github.frankois944.spmForKmp.definition.packageSetting.swiftSetting.SwiftSettingConfig

internal fun getTargetSettings(settings: TargetSettings?): String =
    buildList {
        settings?.run {
            val cSettings = getCSettings(cSettings)
            if (cSettings.isNotEmpty()) {
                add("cSettings: [$cSettings]")
            }
            val cxxSettings = getCXXSettings(cxxSettings)
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
    }.joinToString(",")

private fun getCSettings(settings: CSettingConfig): String =
    buildList {
        settings.defines.forEach {
            add(".define(\"${it.first}\", value: \"${it.second}\")")
        }
        settings.headerSearchPath.forEach {
            add(".headerSearchPath(\"${it}\")")
        }
        if (settings.unsafeFlags.isNotEmpty()) {
            add(".unsafeFlags([${settings.unsafeFlags.joinToString(",") { "\"$it\"" }}])")
        }
    }.joinToString(",")

private fun getCXXSettings(settings: CxxSettingConfig): String =
    buildList {
        settings.defines.forEach {
            add(".define(\"${it.first}\", value: \"${it.second}\")")
        }
        settings.headerSearchPath.forEach {
            add(".headerSearchPath(\"${it}\")")
        }
        if (settings.unsafeFlags.isNotEmpty()) {
            add(".unsafeFlags([${settings.unsafeFlags.joinToString(",") { "\"$it\"" }}])")
        }
    }.joinToString(",")

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
            add(".interoperabilityMode(\".${it}\")")
        }
        settings.swiftLanguageMode?.let {
            add(".swiftLanguageMode(\"${it}\")")
        }
    }.joinToString(",")

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
    }.joinToString(",")
