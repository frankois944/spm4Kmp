package fr.frankois944.spm.kmp.plugin

import fr.frankois944.spm.kmp.plugin.definition.PackageRootDefinitionExtension
import fr.frankois944.spm.kmp.plugin.tasks.GenerateManifestTask
import io.kotest.core.spec.style.WordSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import org.gradle.internal.cc.base.logger
import org.gradle.testfixtures.ProjectBuilder
import java.io.File
import kotlin.io.path.Path

class GenerateManifestPluginTest :
    WordSpec({
        val gradleFile = "build.gradle.kts"
        "Basic check" should {
            val testProjectDir = tempdir()
            "plugin is applied correctly to the project" {
                val project = ProjectBuilder.builder().build()
                project.pluginManager.apply(pluginName)

                assert(project.tasks.getByName("generateSwiftPackage") is GenerateManifestTask)
            }
            "extension swiftPackageConfig is created correctly" {
                val project = ProjectBuilder.builder().build()
                project.pluginManager.apply(pluginName)

                project.extensions.getByName("swiftPackageConfig") shouldNotBe null
            }
            "parameters are passed correctly from extension to task" {
                val project = ProjectBuilder.builder().build()
                project.pluginManager.apply(pluginName)
                (project.extensions.getByName("swiftPackageConfig") as PackageRootDefinitionExtension).apply {
                    customPackageSourcePath = "${testProjectDir.path}/src/spm/"
                    minIos = "12.1"
                    minMacos = "10.13"
                }
                val task = project.tasks.getByName("generateSwiftPackage") as GenerateManifestTask
                task.generatedPackageDirectory shouldBeEqual "${testProjectDir.path}/src/input/"
                task.minIos shouldBeEqual "12.1"
            }
            "Generate the manifest" should {
                "Create package manifest" {
                    File(testProjectDir, gradleFile)
                        .writeText(
                            generateBuildFile(
                                packageTestContent1(testProjectDir.path),
                            ),
                        )

                    logger.warn(File(testProjectDir, gradleFile).readText())
                    val gradleResult = executeGradleRun("generateSwiftPackage", testProjectDir)
                    logger.warn(gradleResult.output)
                    val generatedFileText =
                        Path(testProjectDir.path, "build", "spmKmpPlugin", "input", "Package.swift").toFile()
                    generatedFileText.exists() shouldBe true
                    generatedFileText.readText() shouldContain "https://github.com/firebase/firebase-ios-sdk"
                }
            }
        }
    })
