package io.github.frankois944.spmForKmp.utils

import org.gradle.api.Project
import java.io.File

internal fun Project.getAndCreateFakeDefinitionFile(): File {
    val pluginDir = layout.buildDirectory.asFile
        .get()
        .resolve("spmKmpPlugin")
    if (!pluginDir.exists()) {
        pluginDir.mkdirs()
    }
    val defFile = pluginDir.resolve("dummy.def")
    if (defFile.exists()) return defFile
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
    defFile.writeText(content)
    return defFile
}
