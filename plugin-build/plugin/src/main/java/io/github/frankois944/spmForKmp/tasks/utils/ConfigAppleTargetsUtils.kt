package io.github.frankois944.spmForKmp.tasks.utils

import io.github.frankois944.spmForKmp.PLUGIN_NAME
import io.github.frankois944.spmForKmp.config.AppleCompileTarget
import org.gradle.internal.extensions.stdlib.capitalized

internal fun getTaskName(
    task: String,
    extension: String,
    cinteropTarget: AppleCompileTarget? = null,
) = "${PLUGIN_NAME.capitalized()}Apple${extension.capitalized()}${task.capitalized()}${
    cinteropTarget?.name?.capitalized().orEmpty()
}"

internal fun getCInteropTaskName(
    name: String,
    cinteropTarget: AppleCompileTarget?,
): String = "cinterop${name.capitalized()}${cinteropTarget?.name?.capitalized().orEmpty()}"
