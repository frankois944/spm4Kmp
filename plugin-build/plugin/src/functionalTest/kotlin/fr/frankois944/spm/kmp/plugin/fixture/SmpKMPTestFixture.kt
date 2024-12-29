package fr.frankois944.spm.kmp.plugin.fixture

import com.autonomousapps.kit.AbstractGradleProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.Subproject
import com.autonomousapps.kit.gradle.Imports
import com.autonomousapps.kit.gradle.Plugin
import fr.frankois944.spm.kmp.plugin.definition.SwiftPackageDependencyDefinition
import org.gradle.internal.cc.base.logger

abstract class SmpKMPTestFixture private constructor(
    private val configuration: TestConfiguration,
) : AbstractGradleProject() {
    private var _gradleProject: GradleProject? = null

    val gradleProject: GradleProject
        get() = _gradleProject ?: createProject().also { _gradleProject = it }

    data class TestConfiguration(
        var customPackageSourcePath: String = "src/swift",
        var productName: String = "dummy",
        var minIos: String = "12.0",
        var minMacos: String = "10.13",
        var minTvos: String = "12.0",
        var minWatchos: String = "4.0",
        var toolsVersion: String = "5.9",
        val swiftSources: List<SwiftSource> = emptyList(),
        val kotlinSources: List<KotlinSource> = emptyList(),
        val packages: List<SwiftPackageDependencyDefinition> = emptyList(),
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
            imports = Imports.of("fr.frankois944.spm.kmp.plugin.definition.SwiftPackageDependencyDefinition")
            sourceSets("iosMain", "tvosMain", "watchosMain", "macosMain")
            plugins(
                Plugin.of("org.jetbrains.kotlin.multiplatform", "2.1.0"),
                Plugin(
                    "fr.frankois944.spm.kmp.plugin",
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
    customPackageSourcePath = "${extension.customPackageSourcePath}"
    toolsVersion = "${extension.toolsVersion}"
    productName = "${extension.productName}"
    minIos = "${extension.minIos}"
    minMacos = "${extension.minMacos}"
    minTvos = "${extension.minTvos}"
    minWatchos = "${extension.minWatchos}"
""",
                )
                extension.packages.forEach { definition ->
                    append("    packages.add(\n     ")
                    when (definition) {
                        is SwiftPackageDependencyDefinition.Local -> {
                            append("SwiftPackageDependencyDefinition.Local(")
                            append("path = \"${definition.path}\",")
                            append("names = listOf(\"${definition.names.joinToString(separator = "\", \"")}\"),")
                        }

                        is SwiftPackageDependencyDefinition.LocalBinary -> {
                            append("SwiftPackageDependencyDefinition.LocalBinary(")
                            append("path = \"${definition.path}\",")
                            append("names = listOf(\"${definition.names.joinToString(separator = "\", \"")}\"),")
                        }

                        is SwiftPackageDependencyDefinition.RemoteBinary -> {
                            append("SwiftPackageDependencyDefinition.RemoteBinary(")
                            append("url = \"${definition.url}\",")
                            append("checksum = \"${definition.checksum}\"")
                        }

                        is SwiftPackageDependencyDefinition.RemoteDefinition.Branch -> {
                            append("SwiftPackageDependencyDefinition.RemoteDefinition.Version(")
                            append("url = \"${definition.url}\",")
                            append("names = listOf(\"${definition.names.joinToString(separator = "\", \"")}\"),")
                            append("branch = \"${definition.branch}\",")
                            append("packageName = \"${definition.packageName}\"")
                        }

                        is SwiftPackageDependencyDefinition.RemoteDefinition.Commit -> {
                            append("SwiftPackageDependencyDefinition.RemoteDefinition.Version(")
                            append("url = \"${definition.url}\",")
                            append("names = listOf(\"${definition.names.joinToString(separator = "\", \"")}\"),")
                            append("revision = \"${definition.revision}\",")
                            append("packageName = \"${definition.packageName}\"")
                        }

                        is SwiftPackageDependencyDefinition.RemoteDefinition.Version -> {
                            append("SwiftPackageDependencyDefinition.RemoteDefinition.Version(")
                            append("url = \"${definition.url}\",")
                            append("names = listOf(\"${definition.names.joinToString(separator = "\", \"")}\"),")
                            append("version = \"${definition.version}\",")
                            append("packageName = \"${definition.packageName}\"")
                        }
                    }
                    append(")\n     )\n")
                }
                append("}\n")
            }
        val script =
            """
kotlin {
    listOf(
        //iosX64(),
        //iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.compilations {
            val main by getting {
                cinterops.create("${configuration.productName}")
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
        logger.warn(script)
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

        fun withDependencies(definitions: List<SwiftPackageDependencyDefinition>) =
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
