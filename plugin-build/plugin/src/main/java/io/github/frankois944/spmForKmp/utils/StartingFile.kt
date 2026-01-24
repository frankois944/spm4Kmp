package io.github.frankois944.spmForKmp.utils

import java.io.File

internal object StartingFile {
    private val INITIAL_SWIFT_CONTENT =
        """
        import Foundation
        /**
        This is a starting class to set up your bridge.
        Ensure that your class is public and has the @objcMembers / @objc annotation.
        This file has been created because the folder is empty.
        Ignore this file if you don't need it or set "spmforkmp.disableStartupFile=true" inside your gradle file
        **/

        /**
        @objcMembers public class StartHere: NSObject {
            public override init() {
                super.init()
            }
        }
        **/
        """.trimIndent()

    internal fun createStartingFileIfNeeded(directory: File) {
        val isDirectoryEmpty = directory.listFiles()?.isEmpty() == true
        if (isDirectoryEmpty) {
            val startingFile = directory.resolve("StartYourBridgeHere.swift")
            startingFile.writeText(INITIAL_SWIFT_CONTENT)
        }
    }
}
