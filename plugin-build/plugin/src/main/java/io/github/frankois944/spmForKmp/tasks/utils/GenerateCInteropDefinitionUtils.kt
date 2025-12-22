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
    val namesLowercases = names.map { it.lowercase() }
    return findFilesRecursively(
        directory = path,
        criteria = { file ->
            file.isDirectory && namesLowercases.contains(file.name.lowercase())
        },
        withDirectory = true,
    )
}

internal fun findHeadersModule(
    path: File,
    forTarget: AppleCompileTarget,
): List<File> =
    findFilesRecursively(
        directory = path,
        criteria = { filename ->
            filename.name == "Headers" &&
                filename.path.contains("/${forTarget.xcFrameworkArchName()}/")
        },
        withDirectory = true,
    )

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

internal fun getModulesInBuildDirectory(buildDir: File): List<File> {
    val extensions = listOf("build", "framework")
    return buildDir // get folders with headers for internal dependencies
        .listFiles { file -> extensions.contains(file.extension) || file.name == "Modules" }
        ?.toList() ?: throw RuntimeException("No Module/Framework found in ${buildDir.path}")
}

internal fun GenerateCInteropDefinitionTask.extractModuleNameFromModuleMap(module: String): String? {
    val regex = """module\s+\S+\s+""".toRegex()
    return regex
        .find(module)
        ?.groupValues
        ?.firstOrNull()
        ?.replace("module", "")
        ?.trim()
        ?.also {
            logger.debug("MODULE FOUND {}", it)
        }
}
