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

        fun buildOutputFile(vararg path: String): File =
            Path(
                testProjectDir.path,
                "build",
                "spmKmpPlugin",
                "output",
            ).run {
                var current = this
                for (item in path) {
                    current = current.resolve(item)
                }
                current
            }.toFile()
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
                generatedManifestFileText.readText() shouldContain "https://github.com/krzyzanowskim/CryptoSwift"
                val binaryLib = buildOutputFile("arm64-apple-ios-simulator", "debug", "libproductBinary.a")
                binaryLib.exists() shouldBe true
            }
            "generate cinterop definition" {
                File(testProjectDir, gradleFile)
                    .writeText(
                        generateBuildFile(
                            packageTestContent2(testProjectDir.path),
                        ),
                    )

                val gradleResult = executeGradleRun("generateCInteropDefinition", testProjectDir)
                logger.warn(gradleResult.output)
                val crashlyticsDef = buildOutputFile("FirebaseCrashlytics.def").readText()
                crashlyticsDef.shouldContain("modules = FirebaseCrashlytics")
                crashlyticsDef.shouldContain("package = FirebaseCrashlytics")
                crashlyticsDef.shouldContain("compilerOpts = -ObjC -fmodules -I")

                val firebaseAnalyticsDef = buildOutputFile("FirebaseAnalytics.def").readText()
                firebaseAnalyticsDef.shouldContain("modules = FirebaseAnalytics")
                firebaseAnalyticsDef.shouldContain("package = FirebaseAnalytics")
                firebaseAnalyticsDef.shouldContain("compilerOpts = -fmodules -framework -F")

                val cryptoSwiftDef = buildOutputFile("CryptoSwift.def").readText()
                cryptoSwiftDef.shouldContain("modules = CryptoSwift")
                cryptoSwiftDef.shouldContain("package = CryptoSwift")
                cryptoSwiftDef.shouldContain("compilerOpts = -ObjC -fmodules -I")

                val firebaseCoreDef = buildOutputFile("FirebaseCore.def").readText()
                firebaseCoreDef.shouldContain("modules = FirebaseCore")
                firebaseCoreDef.shouldContain("package = FirebaseCore")
                firebaseCoreDef.shouldContain("compilerOpts = -ObjC -fmodules -I")
            }
        }
    })
