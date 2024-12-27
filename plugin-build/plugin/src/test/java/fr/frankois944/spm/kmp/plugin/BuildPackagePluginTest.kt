package fr.frankois944.spm.kmp.plugin

import io.kotest.core.spec.style.WordSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.gradle.internal.cc.base.logger
import java.io.File
import kotlin.io.path.Path

class BuildPackagePluginTest :
    WordSpec({
        val testProjectDir = tempdir()
        "Compile package" should {
            "compile Swift Package" {
                File(testProjectDir, gradleFile)
                    .writeText(
                        generateBuildFile(
                            packageTestContent1(testProjectDir.path),
                        ),
                    )

                val gradleResult = executeGradleRun("compileSwiftPackage", testProjectDir)
                logger.warn(gradleResult.output)

                val generatedManifestFileText = Path(testProjectDir.path, "build", "spmKmpPlugin", "input", "Package.swift").toFile()
                generatedManifestFileText.exists() shouldBe true
                generatedManifestFileText.readText() shouldContain "https://github.com/firebase/firebase-ios-sdk"
                val generatedOutputDirectory = Path(testProjectDir.path, "build", "spmKmpPlugin", "output", "checkouts").toFile()
                generatedOutputDirectory.exists() shouldBe true
            }
            "generate cinterop" {
                File(testProjectDir, gradleFile)
                    .writeText(
                        generateBuildFile(
                            packageTestContent1(testProjectDir.path),
                        ),
                    )

                val gradleResult = executeGradleRun("generateCInteropDefinition", testProjectDir)
                logger.warn(gradleResult.output)
            }
        }
    })
