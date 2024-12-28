package fr.frankois944.spm.kmp.plugin.fixture

import com.autonomousapps.kit.AbstractGradleProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.Subproject
import com.autonomousapps.kit.gradle.Imports
import com.autonomousapps.kit.gradle.Plugin
import fr.frankois944.spm.kmp.plugin.definition.SwiftPackageDependencyDefinition

abstract class SmpKMPTestFixture private constructor(
    private val configuration: TestConfiguration,
) : AbstractGradleProject() {
    val gradleProject: GradleProject = this.createProject()

    data class TestConfiguration(
        var customPackageSourcePath: String = "src",
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

    protected fun createDefaultProject(): GradleProject {
        val extension =
            TestConfiguration()

        return newGradleProjectBuilder(GradleProject.DslKind.KOTLIN)
            .withRootProject {
                withFile(
                    "gradle.properties",
                    "kotlin.mpp.enableCInteropCommonization=true",
                )
            }.withSubproject("library") {
                setupSources()
                setupGradleConfig(extension)
            }.write()
    }

    private fun Subproject.Builder.setupSources() {
        configuration.swiftSources.forEach { source ->
            withFile(
                "${configuration.productName}/src/main/swift/${source.filename}",
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
                    .build(),
            )
        }
    }

    private fun Subproject.Builder.setupGradleConfig(extension: TestConfiguration) {
        withBuildScript {
            imports = Imports.of("fr.frankois944.spm.kmp.plugin.definition.SwiftPackageDependencyDefinition")
            plugins(
                Plugin.of("org.jetbrains.kotlin.multiplatform", "2.1.0"),
                Plugin("fr.frankois944.spm.kmp.plugin", System.getProperty("com.autonomousapps.plugin-under-test.version")),
            )
            withKotlin(createPluginBlock(extension))
        }
    }

    private fun createPluginBlock(extension: TestConfiguration): String {
        val pluginBlock =
            """
            swiftPackageConfig {
                customPackageSourcePath = "${extension.customPackageSourcePath}"
                toolsVersion = "${extension.toolsVersion}"
                productName = "${extension.productName}"
                minIos = "${extension.minIos}"
                minMacos = "${extension.minMacos}"
                minTvos = "${extension.minTvos}"
                minWatchos = "${extension.minWatchos}"
                packages.add(
                    SwiftPackageDependencyDefinition.RemoteDefinition.Version(
                        url = "https://github.com/krzyzanowskim/CryptoSwift.git",
                        names = listOf("CryptoSwift"),
                        version = "1.8.3",
                    ),
                )
             }
            """.trimIndent()

        return """
            kotlin {
                listOf(
                    iosX64(),
                    iosArm64(),
                    iosSimulatorArm64()
                ).forEach {
                    it.compilations {
                        val main by getting {
                            cinterops.create("${configuration.productName}")
                        }
                    }
                }
            }

            $pluginBlock
            """.trimIndent()
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
                override fun createProject(): GradleProject = createDefaultProject()
            }
    }

    companion object {
        fun builder() = Builder()
    }
}
