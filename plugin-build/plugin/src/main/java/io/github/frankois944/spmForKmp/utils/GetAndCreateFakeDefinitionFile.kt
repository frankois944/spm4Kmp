package io.github.frankois944.spmForKmp.utils

import org.gradle.api.Project
import java.io.File

internal fun Project.getAndCreateFakeDefinitionFile(): File {
    val pathToFile =
        layout.buildDirectory.asFile
            .get()
            .resolve("spmKmpPlugin/dummy.def")
    if (pathToFile.exists()) return pathToFile
    val content =
        """
        # Dummy Definition File
        # This file does nothing but is syntactically valid for cinterop.
        name = DummyLibrary
        headers =
        compilerOpts =
        linkerOpts =
        package = com.example.dummy
        """.trimIndent()
    pathToFile.writeText(content)
    return pathToFile
}
