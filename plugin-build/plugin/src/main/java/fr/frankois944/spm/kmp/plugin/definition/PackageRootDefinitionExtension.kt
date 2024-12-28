package fr.frankois944.spm.kmp.plugin.definition

import org.gradle.api.Project
import javax.inject.Inject

@Suppress("UnnecessaryAbstractClass")
public abstract class PackageRootDefinitionExtension
    @Inject
    constructor(
        project: Project,
    ) {
        public var customPackageSourcePath: String = "${project.projectDir.path}/src/swift"
        public var productName: String = "productBinary"
        public var minIos: String = "12.0"
        public var minMacos: String = "10.13"
        public var minTvos: String = "12.0"
        public var minWatchos: String = "4.0"
        public var toolsVersion: String = "5.9"
        public var debug: Boolean = true
        public val packages: MutableList<SwiftPackageDependencyDefinition> = mutableListOf()

        internal val distinctPackageNames: List<String>
            get() =
                packages
                    .flatMap {
                        it.names
                    }.distinct()
    }
