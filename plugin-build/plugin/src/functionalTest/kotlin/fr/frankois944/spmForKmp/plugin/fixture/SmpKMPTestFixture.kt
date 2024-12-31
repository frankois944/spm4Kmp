package fr.frankois944.spmForKmp.plugin.fixture

import com.autonomousapps.kit.AbstractGradleProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.Subproject
import com.autonomousapps.kit.gradle.Imports
import com.autonomousapps.kit.gradle.Plugin
import fr.frankois944.spmForKmp.plugin.CompileTarget
import fr.frankois944.spmForKmp.plugin.definition.SwiftDependency
import org.gradle.internal.cc.base.logger

abstract class SmpKMPTestFixture private constructor(
    private val configuration: TestConfiguration,
) : AbstractGradleProject() {
    private var _gradleProject: GradleProject? = null

    val gradleProject: GradleProject
        get() = _gradleProject ?: createProject().also { _gradleProject = it }

    data class TestConfiguration(
        var customPackageSourcePath: String = "src/swift",
        var cinteropsName: String = "dummy",
        var minIos: String = "12.0",
        var minMacos: String = "10.13",
        var minTvos: String = "12.0",
        var minWatchos: String = "4.0",
        var toolsVersion: String = "5.9",
        var targets: List<CompileTarget> = listOf(CompileTarget.iosArm64, CompileTarget.iosSimulatorArm64),
        val swiftSources: List<SwiftSource> = emptyList(),
        val kotlinSources: List<KotlinSource> = emptyList(),
        val packages: List<SwiftDependency> = emptyList(),
    )

    protected abstract fun createProject(): GradleProject

    protected fun createDefaultProject(extension: TestConfiguration): GradleProject =
        newGradleProjectBuilder(GradleProject.DslKind.KOTLIN)
            .withRootProject {
                withFile(
                    "gradle.properties",
                    "kotlin.mpp.enableCInteropCommonization=true",
                )
            }.withSubproject("library") {
                setupSources()
                setupGradleConfig(extension)
            }.write()

    private fun Subproject.Builder.setupSources() {
        configuration.swiftSources.forEach { source ->
            withFile(
                "src/swift/${source.filename}",
                source.content,
            )
        }
        val kotlinSources =
            configuration.kotlinSources.ifEmpty {
                listOf(KotlinSource.default())
            }
        kotlinSources.forEach { source ->
            sources.add(
                Source
                    .kotlin(source.content)
                    .withPath(source.packageName, source.className)
                    .withSourceSet("iosMain")
                    .build(),
            )
        }
    }

    private fun Subproject.Builder.setupGradleConfig(extension: TestConfiguration) {
        withBuildScript {
            imports = Imports.of("fr.frankois944.spmForKmp.plugin.definition.SwiftDependency")
            plugins(
                Plugin.of("org.jetbrains.kotlin.multiplatform", "2.1.0"),
                Plugin(
                    "fr.frankois944.spmForKmp.plugin",
                    System.getProperty("com.autonomousapps.plugin-under-test.version"),
                ),
            )
            withKotlin(createPluginBlock(extension))
        }
    }

    private fun createPluginBlock(extension: TestConfiguration): String {
        val pluginBlock =
            buildString {
                append(
                    """
swiftPackageConfig {
    create("${extension.cinteropsName}") {
    customPackageSourcePath = "${extension.customPackageSourcePath}"
    toolsVersion = "${extension.toolsVersion}"
    minIos = "${extension.minIos}"
    minMacos = "${extension.minMacos}"
    minTvos = "${extension.minTvos}"
    minWatchos = "${extension.minWatchos}"
""",
                )
                extension.packages.forEach { definition ->
                    append("    dependency(\n     ")
                    when (definition) {
                        is SwiftDependency.Package.Local -> {
                            append("SwiftDependency.Package.Local(")
                            append("path = \"${definition.path}\",")
                            append("names = listOf(\"${definition.names.joinToString(separator = "\", \"")}\"),")
                            append("packageName = \"${definition.packageName}\",")
                            append("exportToKotlin = ${definition.exportToKotlin}")
                        }

                        is SwiftDependency.Binary.Local -> {
                            append("SwiftDependency.Binary.Local(")
                            append("path = \"${definition.path}\",")
                            append("packageName = \"${definition.packageName}\",")
                            append("exportToKotlin = ${definition.exportToKotlin}")
                        }

                        is SwiftDependency.Binary.Remote -> {
                            append("SwiftDependency.Binary.Remote(")
                            append("url = \"${definition.url}\",")
                            append("checksum = \"${definition.checksum}\",")
                            append("packageName = \"${definition.packageName}\",")
                            append("exportToKotlin = ${definition.exportToKotlin}")
                        }

                        is SwiftDependency.Package.Remote.Branch -> {
                            append("SwiftDependency.Package.Remote.Branch(")
                            append("url = \"${definition.url}\",")
                            append("names = listOf(\"${definition.names.joinToString(separator = "\", \"")}\"),")
                            append("branch = \"${definition.branch}\",")
                            append("packageName = \"${definition.packageName}\",")
                            append("exportToKotlin = ${definition.exportToKotlin}")
                        }

                        is SwiftDependency.Package.Remote.Commit -> {
                            append("SwiftDependency.Package.Remote.Commit(")
                            append("url = \"${definition.url}\",")
                            append("names = listOf(\"${definition.names.joinToString(separator = "\", \"")}\"),")
                            append("revision = \"${definition.revision}\",")
                            append("packageName = \"${definition.packageName}\",")
                            append("exportToKotlin = ${definition.exportToKotlin}")
                        }

                        is SwiftDependency.Package.Remote.Version -> {
                            append("SwiftDependency.Package.Remote.Version(")
                            append("url = \"${definition.url}\",")
                            append("names = listOf(\"${definition.names.joinToString(separator = "\", \"")}\"),")
                            append("version = \"${definition.version}\",")
                            append("packageName = \"${definition.packageName}\",")
                            append("exportToKotlin = ${definition.exportToKotlin}")
                        }
                    }
                    append(")\n     )\n")
                }
                append("}\n}\n")
            }
        val targets = configuration.targets.joinToString(separator = ",") { "$it()" }
        val script =
            """
kotlin {
    listOf(
       $targets
    ).forEach {
        it.compilations {
            val main by getting {
                cinterops.create("${configuration.cinteropsName}")
            }
        }
        it.binaries.framework {
            baseName = "shared"
            isStatic = true
        }
    }
}
$pluginBlock
            """.trimIndent()
        logger.debug(script)
        return script
    }

    class Builder {
        private var config = TestConfiguration()

        fun withSwiftSources(vararg sources: SwiftSource) =
            apply {
                config = config.copy(swiftSources = sources.toList())
            }

        fun withKotlinSources(vararg sources: KotlinSource) =
            apply {
                config = config.copy(kotlinSources = sources.toList())
            }

        fun withTargets(vararg targets: CompileTarget) =
            apply {
                config = config.copy(targets = targets.toList())
            }

        fun withDependencies(definitions: List<SwiftDependency>) =
            apply {
                config = config.copy(packages = definitions)
            }

        fun build(): SmpKMPTestFixture =
            object : SmpKMPTestFixture(config) {
                override fun createProject(): GradleProject = createDefaultProject(config)
            }
    }

    companion object {
        fun builder() = Builder()
    }
}
