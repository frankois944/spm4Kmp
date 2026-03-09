package io.github.frankois944.spmForKmp.tasks.utils

import org.gradle.api.Project

internal val Project.isTraceEnabled: Boolean
    get() = project.enableTracing()
