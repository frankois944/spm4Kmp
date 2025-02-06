package io.github.frankois944.spmForKmp.utils

import java.io.File

private val visited = mutableSetOf<String>()

// Method to find files with the specified name in a directory recursively
@Suppress("NestedBlockDepth")
internal fun findFilesRecursively(
    directory: File,
    criteria: (File) -> Boolean,
    withDirectory: Boolean = false,
    withFirstTime: Boolean = true,
): List<File> {
    val result = mutableListOf<File>()

    if (withFirstTime) {
        visited.clear()
    }

    val canonicalPath = directory.canonicalPath
    if (canonicalPath !in visited && directory.exists() && directory.isDirectory()) {
        visited.add(canonicalPath)
        directory.listFiles()?.let { files ->
            if (withDirectory && criteria(directory)) {
                result.add(directory)
            }
            for (file in files) {
                if (file.isDirectory) {
                    result.addAll(findFilesRecursively(file, criteria, withDirectory, false))
                } else if (criteria(file)) {
                    result.add(file)
                }
            }
        }
    }
    return result
}
