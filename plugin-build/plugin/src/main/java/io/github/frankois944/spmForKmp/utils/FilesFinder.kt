package io.github.frankois944.spmForKmp.utils

import java.io.File
import java.nio.file.Files
import kotlin.streams.toList

internal fun findFilesRecursively(
    directory: File,
    criteria: (File) -> Boolean,
    withDirectory: Boolean = false,
    @Suppress("UNUSED_PARAMETER") withFirstTime: Boolean = true,
): List<File> {
    if (!directory.exists() || !directory.isDirectory) return emptyList()

    return Files
        .walk(directory.toPath())
        .map { it.toFile() }
        .filter { file ->
            if (file.isDirectory) {
                withDirectory && criteria(file)
            } else {
                criteria(file)
            }
        }.toList()
}
