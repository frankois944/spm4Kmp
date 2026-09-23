package io.github.frankois944.spmForKmp.tasks.utils

import io.github.frankois944.spmForKmp.config.AppleCompileTarget
import io.github.frankois944.spmForKmp.config.SpmBuildSystem
import org.gradle.internal.extensions.stdlib.capitalized
import java.io.File

/**
 * A module SwiftPM built, as the cinterop definition step needs to see it.
 *
 * [buildDir] is what the `-F`/`libraryPaths` entries point at, [moduleMap] the module map to read
 * the module name from, and [headerSearchPaths] the directories holding the generated
 * `<Module>-Swift.h` headers.
 */
internal data class BuiltModule(
    val name: String,
    val isFramework: Boolean,
    val buildDir: File,
    val moduleMap: File?,
    val headerSearchPaths: List<File>,
)

/**
 * Where a build system puts what it builds inside the scratch directory.
 *
 * The two layouts share nothing: `native` writes one directory per triple, holding a `.build`
 * directory per module with its module map and generated headers inside it, while `swiftbuild`
 * writes a single `out/Products/<Config>[-<sdk>]` directory holding only the linked products,
 * and puts the module maps and generated headers under `out/Intermediates.noindex`.
 */
internal sealed interface SpmBuildLayout {
    /** The directory holding `lib<Product>.a`, the resource bundles and the built frameworks. */
    val productsDirectory: File

    /** The modules found in the scratch directory, keyed by the name SwiftPM gave them. */
    fun builtModules(): List<BuiltModule>

    /**
     * The compiler flags clang needs to see the built modules, beyond the include paths.
     *
     * clang only discovers a module map implicitly when it is named `module.modulemap`, so a
     * layout that names them otherwise has to point at each one explicitly.
     */
    fun moduleVisibilityCompilerOpts(): List<String>

    /**
     * Everything the compile step produces that the cinterop step then reads.
     *
     * These are the compile task's declared outputs, so a build cache entry carries them: the
     * generated definitions reference them by absolute path, and a cache hit that restored only
     * some of them would leave the cinterop step pointing at files that are not there.
     */
    fun compileOutputDirectories(): List<File>
}

internal fun spmBuildLayout(
    buildSystem: SpmBuildSystem,
    packageScratchDir: File,
    target: AppleCompileTarget,
    buildMode: String,
): SpmBuildLayout =
    when (buildSystem) {
        SpmBuildSystem.NATIVE -> NativeBuildLayout(packageScratchDir, target, buildMode)
        SpmBuildSystem.SWIFTBUILD -> SwiftBuildLayout(packageScratchDir, target, buildMode)
    }

private val nativeModuleMapCandidates =
    listOf(
        "module.modulemap",
        "include/module.modulemap",
        "Modules/module.modulemap",
        "Modules/include/module.modulemap",
    )

/**
 * `<scratch>/<arch>-apple-<os>[-simulator]/<mode>`, one directory per triple.
 */
internal class NativeBuildLayout(
    packageScratchDir: File,
    target: AppleCompileTarget,
    buildMode: String,
) : SpmBuildLayout {
    override val productsDirectory: File =
        packageScratchDir
            .resolve(target.packageBuildDirName())
            .resolve(buildMode)

    override fun builtModules(): List<BuiltModule> =
        productsDirectory
            .listFiles { file ->
                val ext = file.extension
                ext == "build" || ext == "framework" || file.name == "Modules"
            }?.map { dir ->
                BuiltModule(
                    name = dir.nameWithoutExtension,
                    isFramework = dir.extension == "framework",
                    buildDir = dir,
                    moduleMap =
                        nativeModuleMapCandidates
                            .map(dir::resolve)
                            .firstOrNull { it.exists() },
                    headerSearchPaths = listOf(dir.resolve("include")),
                )
            } ?: throw SpmBuildLayoutException("No Module/Framework found in ${productsDirectory.path}")

    /** Nothing to add: every module map is a `module.modulemap` on an include path. */
    override fun moduleVisibilityCompilerOpts(): List<String> = emptyList()

    /** The module maps and generated headers live inside the build directory. */
    override fun compileOutputDirectories(): List<File> = listOf(productsDirectory)
}

/**
 * `<scratch>/out/...`, one directory per configuration and SDK — the architecture does not appear
 * in any of these paths, so two targets sharing an SDK overwrite each other's products and each
 * needs its own scratch directory.
 *
 * The SDK suffix is omitted for macOS: the products land in `Products/Release`, not
 * `Products/Release-macosx`, and likewise for the generated module maps.
 */
internal class SwiftBuildLayout(
    packageScratchDir: File,
    private val target: AppleCompileTarget,
    buildMode: String,
) : SpmBuildLayout {
    private val configurationName: String =
        buildMode.capitalized() +
            if (target.sdk() == MACOS_SDK) "" else "-${target.sdk()}"

    private val outDirectory: File = packageScratchDir.resolve("out")

    override val productsDirectory: File =
        outDirectory
            .resolve("Products")
            .resolve(configurationName)

    /**
     * `GeneratedModuleMaps[-<sdk>]`, holding a `<Module>.modulemap` and a `<Module>-Swift.h` per
     * module — flat, rather than nested inside a per-module directory as `native` does.
     */
    private val generatedModuleMapsDirectory: File =
        outDirectory
            .resolve("Intermediates.noindex")
            .resolve(
                "GeneratedModuleMaps" +
                    if (target.sdk() == MACOS_SDK) "" else "-${target.sdk()}",
            )

    private val packageFrameworksDirectory: File = productsDirectory.resolve("PackageFrameworks")

    override fun builtModules(): List<BuiltModule> {
        val moduleMaps =
            generatedModuleMapsDirectory
                .listFiles { file -> file.extension == "modulemap" }
                ?: throw SpmBuildLayoutException(
                    "No module map found in ${generatedModuleMapsDirectory.path}",
                )
        return moduleMaps.map { moduleMap ->
            val name = moduleMap.nameWithoutExtension
            val framework = packageFrameworksDirectory.resolve("$name.framework")
            BuiltModule(
                name = name,
                isFramework = framework.isDirectory,
                buildDir = if (framework.isDirectory) framework else productsDirectory,
                moduleMap = moduleMap,
                headerSearchPaths = listOf(generatedModuleMapsDirectory),
            )
        }
    }

    /**
     * `swiftbuild` writes `<Module>.modulemap`, which clang never picks up from an include path,
     * so every module map is passed explicitly — including the dependencies', which the bridge
     * imports transitively.
     */
    override fun moduleVisibilityCompilerOpts(): List<String> =
        generatedModuleMapsDirectory
            .listFiles { file -> file.extension == "modulemap" }
            .orEmpty()
            .sortedBy { it.name }
            .map { "-fmodule-map-file=\"${it.absolutePath}\"" }

    /**
     * The module maps sit outside the products directory, under `Intermediates.noindex`, so they
     * have to be named as an output of their own.
     */
    override fun compileOutputDirectories(): List<File> =
        listOf(productsDirectory, generatedModuleMapsDirectory)

    private companion object {
        const val MACOS_SDK = "macosx"
    }
}

internal class SpmBuildLayoutException(
    message: String,
) : RuntimeException(message)
