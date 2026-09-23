package io.github.frankois944.spmForKmp.tasks.utils

import java.io.File

/*
 * The parts of a generated definition file that depend on `publishSafe`.
 *
 * `cinterop` copies the definition file verbatim into the klib manifest, so in publishSafe mode
 * nothing here may point at the machine running the build.
 */

/**
 * The `libraryPaths` line of a definition file, pointing at the Swift package scratch directory
 * of the current machine.
 *
 * It cannot simply be dropped in publishSafe mode: `cinterop` resolves the archive named by
 * `staticLibraries` through it, at generation time, to embed the archive into the klib
 * (`Could not find 'lib<name>.a' binary in neither of []` otherwise). So it is kept on the
 * one definition that declares `staticLibraries`, and removed from all the others, where it
 * resolves nothing and only leaks the path of the build machine.
 *
 * @return `null` when the line must be omitted.
 */
internal fun definitionLibraryPathsLine(
    publishSafe: Boolean,
    declaresStaticLibraries: Boolean,
    buildDirectory: File,
): String? =
    if (publishSafe && !declaresStaticLibraries) {
        null
    } else {
        "libraryPaths = \"$buildDirectory\""
    }

/**
 * The `linkerOpts` of a framework definition.
 *
 * In publishSafe mode, [frameworkSearchPaths] (local scratch directory, and the xcframework slice
 * of a binary dependency) and [extraLinkers] (the local Xcode toolchain) are kept out of the klib
 * manifest, and [extraLinkers] is not even invoked: these search paths are added to the link tasks
 * of the project owning the Swift package instead, see [addPublishSafeLinkerOptions].
 *
 * @param frameworkSearchPaths the `-F` options, already rendered: `native` copies every framework
 * into the build directory, while `swiftbuild` leaves a binary dependency in its xcframework, so
 * how many directories there are depends on the build system.
 */
internal fun frameworkDefinitionLinkerOpts(
    publishSafe: Boolean,
    frameworkFlag: String,
    frameworkSearchPaths: String,
    extraLinkers: () -> String,
): String =
    if (publishSafe) {
        frameworkFlag
    } else {
        "$frameworkFlag $frameworkSearchPaths ${extraLinkers()}".trim()
    }

/**
 * The `linkerOpts` of a non-framework definition (the bridge, and the modules built as static libraries).
 *
 * In publishSafe mode, only the options declared by the user ([userLinkerOpts]) survive, as they are
 * relocatable; `-F` and [extraLinkers] are left out, and [extraLinkers] is not invoked.
 *
 * @return an empty string when there is no linker option at all.
 */
internal fun nonFrameworkDefinitionLinkerOpts(
    publishSafe: Boolean,
    userLinkerOpts: String,
    buildDirPath: String,
    extraLinkers: () -> String,
): String =
    if (publishSafe) {
        userLinkerOpts.trim()
    } else {
        "-F\"$buildDirPath\" $userLinkerOpts ${extraLinkers()}"
    }

/**
 * Join the lines of a definition file, skipping the omitted (`null`) and blank ones.
 */
internal fun renderDefinition(lines: List<String?>): String =
    lines
        .filterNotNull()
        .filter { it.isNotBlank() }
        .joinToString("\n")
