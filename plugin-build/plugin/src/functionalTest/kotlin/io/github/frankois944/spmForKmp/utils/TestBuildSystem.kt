package io.github.frankois944.spmForKmp.utils

import java.io.File

/**
 * The build system the suite is running against, set by the `functionalTest` task from
 * `-Pspmforkmp.testBuildSystem`.
 *
 * The two lay the scratch directory out differently, so a test asserting on what SwiftPM wrote
 * has to ask which one produced it.
 */
object TestBuildSystem {
    val isSwiftBuild: Boolean
        get() =
            System
                .getProperty("spmForKmp.testBuildSystem")
                .orEmpty()
                .equals("swiftbuild", ignoreCase = true)

    /** Where the dependencies are resolved into, relative to the package scratch directory. */
    fun resolveDir(scratchDir: File): File = if (isSwiftBuild) scratchDir.resolve("shared") else scratchDir

    /**
     * Where a target is built, relative to the package scratch directory.
     *
     * `native` writes `<triple>/<mode>` inside a shared scratch directory; `swiftbuild` needs one
     * scratch directory per target and writes `out/Products/<Config>-<sdk>` inside it.
     */
    fun targetBuildDir(
        scratchDir: File,
        targetName: String,
        triple: String,
        configuration: String,
        sdk: String,
    ): File =
        if (isSwiftBuild) {
            scratchDir
                .resolve(targetName)
                .resolve("out/Products")
                .resolve("${configuration.replaceFirstChar { it.uppercase() }}-$sdk")
        } else {
            scratchDir.resolve(triple).resolve(configuration)
        }
}
