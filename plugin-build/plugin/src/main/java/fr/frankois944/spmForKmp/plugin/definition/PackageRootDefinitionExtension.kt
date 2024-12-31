@file:OptIn(ExperimentalStdlibApi::class)

package fr.frankois944.spmForKmp.plugin.definition

import fr.frankois944.spmForKmp.plugin.definition.SwiftDependency
import org.gradle.api.Project
import javax.inject.Inject
import kotlin.io.path.Path
import kotlin.io.path.pathString

/**
 * Package root definition extension
 *
 * @constructor
 *
 * @param project
 */
@Suppress("UnnecessaryAbstractClass")
public abstract class PackageRootDefinitionExtension
    @Inject
    constructor(
        public val name: String,
        project: Project,
    ) {
        public var customPackageSourcePath: String = Path(project.projectDir.path, "src", "swift").pathString
        public var minIos: String = "12.0"
        public var minMacos: String = "10.13"
        public var minTvos: String = "12.0"
        public var minWatchos: String = "4.0"
        public var toolsVersion: String = "5.9"

    /*
     * Build the package in debug/release
     * this should be set at false for production/release distribution
     */
        public var debug: Boolean = true

        internal var packageDependencies: MutableList<SwiftDependency> = mutableListOf()

        public fun dependency(dependency: SwiftDependency) {
            packageDependencies.add(dependency)
        }
    }