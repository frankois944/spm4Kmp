package io.github.frankois944.spmForKmp.tasks.utils

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.extraProperties

internal val Project.isTraceEnabled: Boolean
    get() =
        project.extraProperties.properties
            .getOrDefault("spmforkmp.enableTracing", false)
            .toString()
            .toBoolean()
