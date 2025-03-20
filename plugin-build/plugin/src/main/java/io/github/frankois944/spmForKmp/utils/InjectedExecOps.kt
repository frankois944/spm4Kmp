package io.github.frankois944.spmForKmp.utils

import org.gradle.process.ExecOperations
import javax.inject.Inject

internal interface InjectedExecOps {
    @get:Inject
    val execOps: ExecOperations
}
