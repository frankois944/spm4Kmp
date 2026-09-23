package io.github.frankois944.spmForKmp.tasks.utils

import io.github.frankois944.spmForKmp.config.SpmBuildSystem
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.extraProperties

internal fun Project.hideLocalPackageMessage() =
    extraProperties.properties
        .getOrDefault("spmforkmp.hideLocalPackageMessage", false)
        .toString()
        .toBoolean()

internal fun Project.enableTracing() =
    extraProperties.properties
        .getOrDefault("spmforkmp.enableTracing", false)
        .toString()
        .toBoolean()

internal fun Project.disableStartupFile() =
    extraProperties.properties
        .getOrDefault("spmforkmp.disableStartupFile", false)
        .toString()
        .toBoolean()

/**
 * The build system set for the whole build with `spmforkmp.buildSystem`, or `null` when unset.
 *
 * An unrecognised value is ignored rather than failing the build: it is an escape hatch, and the
 * plugin's own default is always a working one.
 */
internal fun Project.buildSystemProperty(): SpmBuildSystem? =
    extraProperties.properties["spmforkmp.buildSystem"]
        ?.toString()
        ?.let { value ->
            SpmBuildSystem.entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
                ?: run {
                    logger.warn(
                        "spmForKmp: unknown spmforkmp.buildSystem value '{}', expected one of {}",
                        value,
                        SpmBuildSystem.entries.joinToString { it.name.lowercase() },
                    )
                    null
                }
        }
