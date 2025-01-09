package io.github.frankois944.spmForKmp.xcodeconfig

import org.gradle.api.Project
import java.io.File

internal fun Project.generateXcodeConfig(
    moduleConfigs: List<ModuleConfig>,
    buildDir: File,
    isDebug: Boolean,
) {
    if (moduleConfigs.size > 1) {
        val debugConfig =
            buildString {
                appendLine(
                    """
//
// Generated file from SwiftPackageConfigGenerateCInteropDefinition task of SmpForKmp Gradle Plugin
//
// To complete the export of the selected package.
// You need to update your xcode project by adding the following configuration.
// It's necessary for KMP and also makes them possible to use from the Swift code of your application
//
// You're compiling in ${if (isDebug) "debug" else "release"} mode.
//
// You have two choices :
//   - Add this file as a project configuration (How to : https://developer.apple.com/documentation/xcode/adding-a-build-configuration-file-to-your-project#Map-build-settings-to-a-build-configuration)
//   - Add manually these values to your project
//
                    """.trimIndent(),
                )

                val relativePath =
                    "${'$'}{SRCROOT}/../build/" +
                        buildDir.relativeTo(
                            project.layout.buildDirectory.asFile
                                .get(),
                        )
                appendLine(
                    "FRAMEWORK_SEARCH_PATHS = ${'$'}{FRAMEWORK_SEARCH_PATHS} \"$relativePath\"",
                )
                append("OTHER_LDFLAGS = ${'$'}{OTHER_LDFLAGS} -ObjC")
                moduleConfigs.drop(1).forEach {
                    append(" -framework ${it.buildDir.nameWithoutExtension} ")
                }
            }
        val filename = "spmForKmp.${if (isDebug) "debug" else "release"}.xcconfig"
        val saveTo =
            project.layout.projectDirectory.asFile
                .resolve(filename)
        logger.warn("Spm4Kmp: Mandatory configuration: file://${saveTo.path}")
        saveTo.writeText(debugConfig.trimIndent())
    }
}
