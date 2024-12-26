package fr.frankois944.spm.kmp.plugin

import fr.frankois944.spm.kmp.plugin.definition.PackageRootDefinition
import fr.frankois944.spm.kmp.plugin.tasks.GenerateManifestTask
import io.kotest.core.spec.style.WordSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import org.gradle.internal.cc.base.logger
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import java.io.File
import kotlin.io.path.Path

val pluginName = "fr.frankois944.spm.kmp.plugin"

private fun executeGradleRun(
    task: String,
    testProjectDir: File,
): BuildResult =
    GradleRunner
        .create()
        .withProjectDir(testProjectDir)
        .withArguments(task)
        .withPluginClasspath()
        .build()

private fun generateBuildFile(config: String) =
    """
import fr.frankois944.spm.kmp.plugin.definition.SwiftPackageDependencyDefinition

plugins {
    java
    id("fr.frankois944.spm.kmp.plugin")
}
swiftPackageConfig {
    $config
}
    """.trimIndent()

class KotestPluginTest :
    WordSpec({
        val gradleFile = "build.gradle.kts"
        "Using the Plugin ID" should {
            val testProjectDir = tempdir()
            "Apply the Plugin" {
                val project = ProjectBuilder.builder().build()
                project.pluginManager.apply(pluginName)

                project.plugins.getPlugin(SPMKMPPlugin::class.java) shouldNotBe null
            }
            "Register the 'kotest' extension" {
                val project = ProjectBuilder.builder().build()
                project.pluginManager.apply(SPMKMPPlugin::class.java)
            }
            "plugin is applied correctly to the project" {
                val project = ProjectBuilder.builder().build()
                project.pluginManager.apply(pluginName)

                assert(project.tasks.getByName("generateSwiftPackage") is GenerateManifestTask)
            }
            "extension templateExampleConfig is created correctly" {
                val project = ProjectBuilder.builder().build()
                project.pluginManager.apply(pluginName)

                project.extensions.getByName("swiftPackageConfig") shouldNotBe null
            }
            "parameters are passed correctly from extension to task" {
                val project = ProjectBuilder.builder().build()
                project.pluginManager.apply(pluginName)
                (project.extensions.getByName("swiftPackageConfig") as PackageRootDefinition).apply {
                    generatedPackageDirectory = "${testProjectDir.path}/src/spm/"
                    minIos = "12.1"
                    minMacos = "10.13"
                }

                val task = project.tasks.getByName("generateSwiftPackage") as GenerateManifestTask
                task.generatedPackageDirectory.get() shouldBeEqual "${testProjectDir.path}/src/spm/"
                task.minIos.get() shouldBeEqual "12.1"
            }
            "Generate the Swift Package Manifest" {
                File(testProjectDir, gradleFile)
                    .writeText(
                        generateBuildFile(
                            """
                            generatedPackageDirectory = "${testProjectDir.path}/src/spm"
                            minIos = "12.0"
                            minMacos = "10.13"
                            packages.add(
                                SwiftPackageDependencyDefinition.RemoteDefinition.Version(
                                    url = "https://github.com/firebase/firebase-ios-sdk",
                                    names = listOf("FirebaseAuth", "FirebaseCore", "FirebaseAnalytics"),
                                    packageName = "firebase-ios-sdk",
                                    version = "11.6.0",
                                ),
                            )
                            """.trimIndent(),
                        ),
                    )

                logger.warn(File(testProjectDir, gradleFile).readText())
                val gradleResult = executeGradleRun("generateSwiftPackage", testProjectDir)
                logger.warn(gradleResult.output)

                gradleResult.output shouldContain "targets: [\"productBinary\"]"
                val generatedFileText = Path(testProjectDir.path, "src", "spm", "Package.swift").toFile()
                generatedFileText.exists() shouldBe true
            }
        }
    })
