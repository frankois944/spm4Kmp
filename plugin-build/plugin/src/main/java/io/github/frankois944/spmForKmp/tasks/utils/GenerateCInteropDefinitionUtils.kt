package io.github.frankois944.spmForKmp.tasks.utils

import io.github.frankois944.spmForKmp.SPM_ARTIFACTS_DIR_NAME
import io.github.frankois944.spmForKmp.config.AppleCompileTarget
import io.github.frankois944.spmForKmp.config.ModuleConfig
import io.github.frankois944.spmForKmp.definition.SwiftDependency
import io.github.frankois944.spmForKmp.tasks.apple.generateCInteropDefinition.GenerateCInteropDefinitionTask
import io.github.frankois944.spmForKmp.utils.findFilesRecursively
import java.io.File
import java.nio.file.Path

/**
 * The definition file of a module, following the naming convention shared by the
 * task outputs and the configuration-time prediction: the module at index 0 is the
 * bridge (`<name>_bridge.def`), the following ones are exported dependencies
 * (`<name>.def`).
 *
 * Both call sites MUST use this function, otherwise the declared task outputs and
 * the files wired into the cinterop tasks can drift apart.
 */
internal fun definitionFileOf(
    definitionFolder: File,
    moduleConfig: ModuleConfig,
    index: Int,
): File =
    if (index == 0) {
        definitionFolder.resolve("${moduleConfig.name}_bridge.def")
    } else {
        definitionFolder.resolve("${moduleConfig.name}.def")
    }

/**
 * Computes the module configurations for a cinterop entry without requiring a
 * realized task: the first module is always the bridge product, followed by the
 * exportable dependency products. Used both by [GenerateCInteropDefinitionTask]
 * and at configuration time to predict the definition file names lazily.
 */
internal fun computeModuleConfigs(
    productName: String,
    compilerOpts: List<String>,
    linkerOpts: List<String>,
    packages: List<SwiftDependency>,
): List<ModuleConfig> =
    buildList {
        // the first item must be the product name
        add(
            ModuleConfig(
                name = productName,
                compilerOpts = compilerOpts,
                linkerOpts = linkerOpts,
            ),
        )
        addAll(
            packages
                .filterExportableDependency()
                .flatMap { dependency ->
                    when (dependency) {
                        is SwiftDependency.Package -> {
                            dependency.productsConfig.productPackages
                                .flatMap { product ->
                                    product.products
                                }.map { product ->
                                    ModuleConfig(
                                        name = product.name,
                                        alias = product.alias,
                                        packageName = dependency.packageName,
                                        spmPackageName = dependency.packageName,
                                    )
                                }
                        }

                        is SwiftDependency.Binary -> {
                            listOf(
                                ModuleConfig(
                                    name = dependency.packageName,
                                    spmPackageName = dependency.packageName,
                                    isCLang = dependency.isCLang,
                                ),
                            )
                        }
                    }
                },
        )
    }.distinctBy { it.name }

internal fun findFolders(
    path: File,
    vararg names: String,
): List<File> {
    if (names.isEmpty()) return emptyList()
    return try {
        val namesLowercaseSet = names.mapTo(HashSet(names.size)) { it.lowercase() }
        findFilesRecursively(
            directory = path,
            criteria = { file ->
                // Early exit on non-directory to avoid string operations
                file.isDirectory && namesLowercaseSet.contains(file.name.lowercase())
            },
            withDirectory = true,
        )
    } catch (_: Throwable) {
        emptyList()
    }
}

internal fun findHeadersModule(
    path: File,
    forTarget: AppleCompileTarget,
): List<File> =
    try {
        val targetArchName = "/${forTarget.xcFrameworkArchName()}/"
        findFilesRecursively(
            directory = path,
            criteria = { filename ->
                filename.name == "Headers" &&
                    filename.path.contains(targetArchName)
            },
            withDirectory = true,
        )
    } catch (_: Exception) {
        emptyList()
    }

internal fun getModuleArtifactsPath(
    fromPath: Path,
    productName: String,
    moduleConfig: ModuleConfig,
    target: AppleCompileTarget,
): Path =
    fromPath
        .resolve(SPM_ARTIFACTS_DIR_NAME)
        .resolve(productName.lowercase())
        .resolve(moduleConfig.name)
        .resolve("${moduleConfig.name}.xcframework")
        .resolve(target.xcFrameworkArchName())

internal fun getModulesInBuildDirectory(buildDir: File): List<File> =
    buildDir
        .listFiles { file ->
            val ext = file.extension
            ext == "build" || ext == "framework" || file.name == "Modules"
        }?.toList() ?: throw RuntimeException("No Module/Framework found in ${buildDir.path}")

private val moduleNameRegex = """module\s+(\S+)\s+""".toRegex()

internal fun GenerateCInteropDefinitionTask.extractModuleNameFromModuleMap(module: String): String? =
    moduleNameRegex
        .find(module)
        ?.groupValues
        ?.getOrNull(1)
        ?.also {
            logger.debug("MODULE FOUND {}", it)
        }
