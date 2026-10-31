package io.github.frankois944.spmForKmp.tasks.utils

import io.github.frankois944.spmForKmp.config.AppleCompileTarget
import io.github.frankois944.spmForKmp.definition.SwiftDependency
import java.io.File

/**
 * Resolves a binary dependency straight from its xcframework.
 *
 * The `native` build system copies the slice a target links against into its build directory, as
 * `<Module>.framework`, so the module is found there like any other. `swiftbuild` does not copy
 * anything: the xcframework stays where it is and nothing named `.framework` ever appears in the
 * scratch directory. Binary dependencies are therefore read from the xcframework itself, which
 * works under either build system.
 */
internal fun resolveBinaryModule(
    dependency: SwiftDependency.Binary?,
    identities: List<String>,
    moduleName: String,
    artifactsDir: File,
    target: AppleCompileTarget,
): BuiltModule? =
    xcFrameworkLocations(dependency, identities, moduleName, artifactsDir)
        .firstNotNullOfOrNull { xcFramework ->
            findXcFrameworkSlice(xcFramework, target)?.let { readSlice(it, moduleName) }
        }

/**
 * Where the xcframework of a binary module may be found, most specific first.
 *
 * A binary declared by the DSL with a local path is used in place. Everything else was downloaded
 * and extracted by the resolve step into `artifacts/<identity>/<target>/<target>.xcframework`, and
 * which identity depends on who declared it: a binary the DSL declares belongs to the package the
 * plugin generates, and one declared by a dependency's own manifest — as `firebase-ios-sdk` does
 * for `FirebaseFirestoreInternal` — belongs to that dependency. The candidates are tried in turn
 * rather than worked out, because the module carries both names without saying which applies.
 */
private fun xcFrameworkLocations(
    dependency: SwiftDependency.Binary?,
    identities: List<String>,
    moduleName: String,
    artifactsDir: File,
): List<File> =
    buildList {
        if (dependency is SwiftDependency.Binary.Local) {
            add(File(dependency.path))
        }
        identities
            .filter { it.isNotEmpty() }
            .distinct()
            .forEach { identity ->
                add(
                    artifactsDir
                        .resolve(identity)
                        .resolve(moduleName)
                        .resolve("$moduleName.xcframework"),
                )
            }
    }

/**
 * The directory inside an xcframework holding the build for [target].
 *
 * Slices are named `<platform>-<arch>[_<arch>…][-simulator]`, so the name depends on which
 * architectures the xcframework was built for: the conventional name is tried first, and when the
 * xcframework carries fewer architectures than usual the single slice for the right platform is
 * taken instead. Anything more ambiguous is left unresolved rather than guessed at.
 */
internal fun findXcFrameworkSlice(
    xcFramework: File,
    target: AppleCompileTarget,
): File? {
    val conventional = target.xcFrameworkArchName()
    xcFramework.resolve(conventional).takeIf { it.isDirectory }?.let { return it }

    val platform = conventional.substringBefore('-')
    val wantsSimulator = conventional.endsWith(SIMULATOR_SUFFIX)
    val candidates =
        xcFramework
            .listFiles { file -> file.isDirectory }
            .orEmpty()
            .filter { it.name.startsWith("$platform-") && it.name.endsWith(SIMULATOR_SUFFIX) == wantsSimulator }
    return candidates.singleOrNull()
}

/**
 * Reads a slice, which holds either a framework or a plain library with its headers beside it.
 */
private fun readSlice(
    slice: File,
    moduleName: String,
): BuiltModule? {
    val framework =
        slice
            .listFiles { file -> file.isDirectory && file.extension == "framework" }
            ?.firstOrNull()
    return if (framework != null) {
        BuiltModule(
            name = moduleName,
            isFramework = true,
            buildDir = framework,
            moduleMap = framework.resolve("Modules/module.modulemap").takeIf { it.isFile },
            headerSearchPaths = listOfNotNull(framework.resolve("Headers").takeIf { it.isDirectory }),
        )
    } else {
        slice.resolve("Headers").takeIf { it.isDirectory }?.let { headers ->
            BuiltModule(
                name = moduleName,
                isFramework = false,
                buildDir = slice,
                moduleMap =
                    listOf("module.modulemap", "module.map")
                        .map(headers::resolve)
                        .firstOrNull { it.isFile },
                headerSearchPaths = listOf(headers),
            )
        }
    }
}

private const val SIMULATOR_SUFFIX = "-simulator"
