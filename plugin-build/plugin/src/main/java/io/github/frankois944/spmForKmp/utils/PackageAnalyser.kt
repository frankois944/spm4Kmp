package io.github.frankois944.spmForKmp.utils

import java.io.File

/**
 * Extracts header search paths from a Package.swift file for a specific target.
 *
 * Note: This follows a "fast and lightweight" approach. It assumes standard
 * SPM formatting.
 */
internal class SwiftManifestParser(
    private val manifestFile: File,
) {
    /**
     * Finds all header search paths (internal and public) for the specified target and its dependencies.
     */
    fun extractHeaderSearchPaths(targetName: String): List<String> {
        if (!manifestFile.exists()) return emptyList()
        val content = manifestFile.readText()
        val visitedTargets = mutableSetOf<String>()
        val allPaths = mutableSetOf<String>()

        fun collectRecursive(name: String) {
            if (visitedTargets.contains(name)) return
            visitedTargets.add(name)

            val targetBlock = findTargetBlock(content, name) ?: return

            // 1. Resolve this target's base directory
            val customPathRegex = Regex("""path\s*:\s*"([^"]+)"""")
            val customPath = customPathRegex.find(targetBlock)?.groupValues?.get(1)
            val packageRoot = manifestFile.parentFile
            val targetBaseDir =
                if (customPath != null) {
                    packageRoot.resolve(customPath)
                } else {
                    packageRoot.resolve("Sources").resolve(name)
                }

            // 2. Extract publicHeadersPath
            val publicHeadersRegex = Regex("""publicHeadersPath\s*:\s*"([^"]*)"""") // Note the * for empty string support
            val publicHeadersMatch = publicHeadersRegex.find(targetBlock)

            val publicPath =
                if (publicHeadersMatch != null) {
                    // If it matches, use the captured group (which can be empty)
                    publicHeadersMatch.groupValues[1]
                } else {
                    // SPM default if property is missing
                    "include"
                }

            val absolutePublicPath = targetBaseDir.resolve(publicPath).normalize()
            if (absolutePublicPath.exists()) {
                allPaths.add(absolutePublicPath.absolutePath)
            }

            // 3. Extract local .headerSearchPath("path")
            val searchPathRegex = Regex("""\.headerSearchPath\s*\(\s*"([^"]+)"\s*\)""")
            searchPathRegex.findAll(targetBlock).forEach { match ->
                val relativePath = match.groupValues[1]
                val absolutePath = targetBaseDir.resolve(relativePath).normalize()
                if (absolutePath.exists()) {
                    allPaths.add(absolutePath.absolutePath)
                }
            }

            // 4. Extract and follow dependencies
            extractDependencies(targetBlock).forEach { depName ->
                collectRecursive(depName)
            }
        }

        collectRecursive(targetName)
        return allPaths.toList()
    }

    private fun extractDependencies(targetBlock: String): List<String> {
        // Find the dependencies: [ ... ] block inside the target
        val depsMatch =
            Regex("""dependencies\s*:\s*\[([^]]+)]""", RegexOption.DOT_MATCHES_ALL)
                .find(targetBlock)
        if (depsMatch == null) return emptyList()

        val depsContent = depsMatch.groupValues[1]

        // Match string literals "DepName", or .target(name: "DepName"), or .product(name: "DepName", ...)
        // This is a simplified regex for common SPM dependency patterns
        val depNameRegex = Regex(""""([^"]+)"""")
        return depNameRegex
            .findAll(depsContent)
            .map { it.groupValues[1] }
            // Filter out things that look like package names in .product(name: "Target", package: "Package")
            // In a simple regex, we just take all strings and filter by target existence in the recursive call
            .toList()
    }

    private fun findTargetBlock(
        content: String,
        targetName: String,
    ): String? {
        // Regex to find the start of a target declaration with the specific name
        // Supports both 'name: "target"' and '"target"' (positional)
        val startRegex =
            Regex("""\.(?:target|executableTarget|testTarget)\s*\(\s*(?:name\s*:\s*)?"$targetName"""")
        val match = startRegex.find(content) ?: return null

        val startIndex = match.range.first
        var braceCount = 0
        var foundFirstBrace = false

        // Extract the content until the closing parenthesis of the target function
        for (i in startIndex until content.length) {
            val char = content[i]
            if (char == '(') {
                braceCount++
                foundFirstBrace = true
            } else if (char == ')') {
                braceCount--
            }

            if (foundFirstBrace && braceCount == 0) {
                return content.substring(startIndex, i + 1)
            }
        }
        return null
    }
}
