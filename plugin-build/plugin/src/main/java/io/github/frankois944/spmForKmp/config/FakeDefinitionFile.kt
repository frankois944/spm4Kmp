package io.github.frankois944.spmForKmp.config

import org.gradle.api.Project
import java.io.File

internal fun Project.getAndCreateFakeDefinitionFile(): File {
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
    val pathToFile =
        layout.buildDirectory.asFile
            .get()
            .resolve("spmKmpPlugin/dummy.def")
    pathToFile.writeText(content)
    return pathToFile
}
