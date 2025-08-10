package io.github.frankois944.spmForKmp.tasks.utils

import io.github.frankois944.spmForKmp.config.AppleCompileTarget
import io.github.frankois944.spmForKmp.config.ModuleConfig
import io.github.frankois944.spmForKmp.tasks.apple.GenerateCInteropDefinitionTask
import io.github.frankois944.spmForKmp.utils.extractTargetBlocks
import io.github.frankois944.spmForKmp.utils.findFilesRecursively
import org.gradle.internal.cc.base.logger
import java.io.File

internal fun findIncludeFolders(path: File): List<File> =
    findFilesRecursively(
        directory = path,
        criteria = { file ->
            file.isDirectory && file.name == "include"
        },
        withDirectory = true,
    )

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

internal fun getModulesInBuildDirectory(buildDir: File): List<File> {
    val extensions = listOf("build", "framework")
    return buildDir // get folders with headers for internal dependencies
        .listFiles { file -> extensions.contains(file.extension) || file.name == "Modules" }
        ?.toList() ?: throw RuntimeException("No Module/Framework found in ${buildDir.path}")
}

internal fun GenerateCInteropDefinitionTask.extractPublicHeaderFromCheckout(
    fromDir: File,
    module: ModuleConfig,
): Set<File> {
    logger.debug("Looking for public header for {}", module.name)

    val checkoutsDir = "checkouts"
    val packageSwift = "Package.swift"
    val sourcesDir = "Sources"

    val packageDir = fromDir.resolve(checkoutsDir).resolve(module.packageName)
    val manifestFile = packageDir.resolve(packageSwift)
    val result = mutableSetOf<File>()

    if (!manifestFile.exists()) {
        logger.debug("No manifest found at {}", manifestFile.path)
        return result
    }

    val content = manifestFile.readText()
    val targets = extractTargetBlocks(content)

    targets.forEach { target ->
        val name = extractFirstMatch(target, """name:\s*"([^"]+)"""")
        val targetPath = extractFirstMatch(target, """path:\s*"([^"]+)"""")
        logger.debug("targetPath {}", targetPath)
        val publicHeadersPath = extractFirstMatch(target, """publicHeadersPath:\s*"([^"]+)"""")
        logger.debug("publicHeadersPath: {}", publicHeadersPath)
        var resolvedIncludeDir =
            if (targetPath != null) {
                packageDir.resolve(targetPath)
            } else {
                packageDir.resolve(sourcesDir).resolve(name ?: module.name)
            }

        if (publicHeadersPath != null) {
            resolvedIncludeDir = resolvedIncludeDir.resolve(publicHeadersPath)
        }

        logger.debug("resolvedIncludeDir: {}", resolvedIncludeDir)
        if (resolvedIncludeDir.exists()) {
            result.add(resolvedIncludeDir)
        } else {
            logger.debug("PUBLIC HEADER NOT FOUND AT: {} FOR : {}", resolvedIncludeDir, module.name)
        }
    }
    return result
}

internal fun extractFirstMatch(
    input: String,
    pattern: String,
): String? = Regex(pattern).find(input)?.groupValues?.getOrNull(1)

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
