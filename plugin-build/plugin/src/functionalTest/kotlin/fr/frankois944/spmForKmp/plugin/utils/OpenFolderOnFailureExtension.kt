package fr.frankois944.spmForKmp.plugin.utils

import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.TestWatcher
import java.awt.Desktop
import java.io.File

class OpenFolderOnFailureExtension(
    private val folderPath: () -> String,
) : TestWatcher {
    override fun testFailed(
        context: ExtensionContext,
        cause: Throwable?,
    ) {
        val folder = File(folderPath())
        if (folder.exists() && folder.isDirectory) {
            try {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(folder)
                } else {
                    println("Desktop actions are not supported on this system.")
                }
            } catch (e: Exception) {
                println("Failed to open folder: ${e.message}")
            }
        } else {
            println("Folder does not exist: $folderPath")
        }
    }
}
