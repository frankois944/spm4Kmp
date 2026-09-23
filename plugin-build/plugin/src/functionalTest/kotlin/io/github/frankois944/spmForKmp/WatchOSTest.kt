package io.github.frankois944.spmForKmp

import com.autonomousapps.kit.GradleBuilder
import com.autonomousapps.kit.truth.TestKitTruth.Companion.assertThat
import io.github.frankois944.spmForKmp.config.AppleCompileTarget
import io.github.frankois944.spmForKmp.fixture.KotlinSource
import io.github.frankois944.spmForKmp.fixture.SmpKMPTestFixture
import io.github.frankois944.spmForKmp.fixture.SwiftSource
import io.github.frankois944.spmForKmp.utils.BaseTest
import io.github.frankois944.spmForKmp.utils.TestBuildSystem
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class WatchOSTest : BaseTest() {
    @Test
    fun `build for watchOS`() {
        val fixture =
            SmpKMPTestFixture
                .builder()
                .withBuildPath(testProjectDir.root.absolutePath)
                .withTargets(
                    AppleCompileTarget.watchosArm32,
                    AppleCompileTarget.watchosArm64,
                    AppleCompileTarget.watchosX64,
                    AppleCompileTarget.watchosSimulatorArm64
                )
                .withRawTargetBlock(
                    KotlinSource.of(
                        content =
                            """
                            it.swiftPackageConfig(cinteropName = "dummy") {
                            }
                            """.trimIndent(),
                    ),
                )
                .withKotlinSources(
                    KotlinSource.of(
                        imports = listOf("dummy.*"),
                        content =
                            """
                            @kotlinx.cinterop.ExperimentalForeignApi
                            val view = ObjectCls()
                            """.trimIndent(),
                    ),
                )
                .withSwiftSources(
                    SwiftSource.of(
                        content =
                            """
                            import WatchKit

                            // Équivalent de UIViewController sur iOS
                            class MyController: WKInterfaceController {
                                override func awake(withContext context: Any?) { }
                                override func willActivate() { }
                                override func didDeactivate() { }
                            }

                             @objc public class ObjectCls: NSObject {}
                            """.trimIndent(),
                    ),
                ).build()

        val result =
            GradleBuilder
                .runner(fixture.gradleProject.rootDir, "build")
                .build()

        // Then
        assertThat(result).task(":library:build").succeeded()
    }

    // https://github.com/frankois944/spm4Kmp/issues/333
    @Test
    fun `build for watchOS simulator arm64 with a binary xcframework`() {
        val xcFrameworkDirectory = File("src/functionalTest/resources/DummyFramework.xcframework")
        val fixture =
            SmpKMPTestFixture
                .builder()
                .withBuildPath(testProjectDir.root.absolutePath)
                .withTargets(AppleCompileTarget.watchosSimulatorArm64)
                // The default (4.0) is kept low for watchosArm32/armv7k, but the `swiftbuild`
                // build system validates it against the watchOS SDK, which supports 9.0 and up.
                .withMinWatchOs("9.0")
                .withRawDependencies(
                    KotlinSource.of(
                        content =
                            """
                            localBinary(
                                path = "${xcFrameworkDirectory.absolutePath}",
                                packageName = "DummyFramework",
                                exportToKotlin = true,
                            )
                            """.trimIndent(),
                    ),
                ).withKotlinSources(
                    KotlinSource.of(
                        imports = listOf("DummyFramework.DummyFrameworkVersionNumber"),
                    ),
                ).withSwiftSources(
                    SwiftSource.of(
                        content =
                            """
                            import Foundation
                            import DummyFramework
                            @objc public class MySwiftClass: NSObject {
                            }
                            """.trimIndent(),
                    ),
                ).build()

        val result =
            GradleBuilder
                .runner(fixture.gradleProject.rootDir, "build")
                .build()

        assertThat(result).task(":library:build").succeeded()
        if (TestBuildSystem.isSwiftBuild) {
            // `swiftbuild` copies nothing: the binary stays in its xcframework and the generated
            // definition points at the slice for this triple.
            val definitions =
                fixture.gradleProject.rootDir
                    .walk()
                    .filter { it.isFile && it.name == "DummyFramework.def" }
                    .toList()
            assertTrue(definitions.isNotEmpty(), "no DummyFramework definition was generated")
            assertTrue(
                definitions.any { it.readText().contains(".xcframework") },
                "the definition must point at the xcframework slice:\n" +
                    definitions.joinToString("\n") { it.readText() },
            )
        } else {
            // `native` copies the binary slice into the build directory of the arm64 simulator triple
            assertTrue(
                fixture.gradleProject.rootDir
                    .walk()
                    .any {
                        it.isDirectory &&
                            it.name == "DummyFramework.framework" &&
                            it.parentFile?.parentFile?.name == "arm64-apple-watchos-simulator"
                    },
            )
        }
    }
}
