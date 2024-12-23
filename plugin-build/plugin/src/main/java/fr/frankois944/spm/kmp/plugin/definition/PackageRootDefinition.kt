package fr.frankois944.spm.kmp.plugin.definition

import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.options.Option
import java.io.File
import javax.inject.Inject

@Suppress("UnnecessaryAbstractClass")
public abstract class PackageRootDefinition
    @Inject
    constructor(
        project: Project,
    ) {
        @get:Input
        @get:Optional
        @get:Option(option = "manifest", description = "The path where the plugin build the sources")
        public var generatedPackageDirectory: File = File("${project.projectDir}/src/spm")

        @get:Input
        @get:Optional
        @get:Option(option = "productName", description = "The product package name")
        public var productName: String = "productBinary"

        @get:Input
        @get:Optional
        public var minIos: String = "12.0"

        @get:Input
        @get:Optional
        public var minMacos: String = "10.13"

        @get:Input
        @get:Optional
        public var minTvos: String = "12.0"

        @get:Input
        @get:Optional
        public var minWatchos: String = "4.0"

        @get:Input
        @get:Optional
        public var toolsVersion: String = "5.9"

        @get:Input
        @get:Optional
        public val dependencies: MutableList<SwiftPackageDependencyDefinition> = mutableListOf()
    }
