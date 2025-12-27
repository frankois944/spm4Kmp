package io.github.frankois944.spmForKmp.tasks.utils

import io.github.frankois944.spmForKmp.config.AppleCompileTarget
import io.github.frankois944.spmForKmp.config.ModuleConfig
import io.github.frankois944.spmForKmp.tasks.apple.generateCInteropDefinition.GenerateCInteropDefinitionTask
import io.github.frankois944.spmForKmp.utils.extractTargetBlocks
import io.github.frankois944.spmForKmp.utils.findFilesRecursively
import java.io.File
import java.nio.file.Path

internal fun findFolders(
    path: File,
    vararg names: String,
): List<File> {
    if (names.isEmpty()) return emptyList()
    val namesLowercaseSet = names.mapTo(HashSet(names.size)) { it.lowercase() }
    return findFilesRecursively(
        directory = path,
        criteria = { file ->
            // Early exit on non-directory to avoid string operations
            file.isDirectory && namesLowercaseSet.contains(file.name.lowercase())
        },
        withDirectory = true,
    )
}

internal fun findHeadersModule(
    path: File,
    forTarget: AppleCompileTarget,
): List<File> {
    val targetArchName = "/${forTarget.xcFrameworkArchName()}/"
    return findFilesRecursively(
        directory = path,
        criteria = { filename ->
            filename.name == "Headers" &&
                filename.path.contains(targetArchName)
        },
        withDirectory = true,
    )
}

internal fun getModuleArtifactsPath(
    fromPath: Path,
    productName: String,
    moduleConfig: ModuleConfig,
    target: AppleCompileTarget,
): Path =
    fromPath
        .resolve("artifacts")
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
