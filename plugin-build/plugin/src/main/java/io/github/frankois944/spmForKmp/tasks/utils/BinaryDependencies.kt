package io.github.frankois944.spmForKmp.tasks.utils

import io.github.frankois944.spmForKmp.definition.SwiftDependency
import java.io.File

/**
 * What is needed to locate the xcframework of a binary dependency, see
 * [binaryFrameworkSearchPaths].
 *
 * @property artifactsDir where the resolve step extracts the remote binaries.
 * @property productName the name of the package the plugin generates, which is the identity a
 * binary declared by the DSL is extracted under.
 * @property declared every dependency of the package; the binary ones are picked out of it.
 */
internal data class BinaryDependencies(
    val artifactsDir: File,
    val productName: String,
    val declared: List<SwiftDependency>,
)
