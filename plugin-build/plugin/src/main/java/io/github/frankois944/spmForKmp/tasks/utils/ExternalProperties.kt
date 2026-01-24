package io.github.frankois944.spmForKmp.tasks.utils

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
