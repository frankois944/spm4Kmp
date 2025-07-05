package io.github.frankois944.spmForKmp

import com.autonomousapps.kit.GradleBuilder
import com.autonomousapps.kit.truth.TestKitTruth.Companion.assertThat
import io.github.frankois944.spmForKmp.config.AppleCompileTarget
import io.github.frankois944.spmForKmp.fixture.KotlinSource
import io.github.frankois944.spmForKmp.fixture.SmpKMPTestFixture
import io.github.frankois944.spmForKmp.fixture.SwiftSource
import io.github.frankois944.spmForKmp.utils.BaseTest
import org.junit.jupiter.api.Test
import java.io.File

class NullablePlatformTest : BaseTest() {
    @Test
    fun `build with nullable platform`() {
        val localPackageDirectory = File("src/functionalTest/resources/LocalSourceDummyFramework")

        // Given
        val fixture =
            SmpKMPTestFixture
                .builder()
                .withBuildPath(testProjectDir.root.absolutePath)
                .also {
                    if (isCI) {
                        it.withTargets(
                            AppleCompileTarget.macosArm64,
                            AppleCompileTarget.watchosSimulatorArm64,
                            AppleCompileTarget.tvosSimulatorArm64,
                            AppleCompileTarget.iosArm64,
                        )
                    } else {
                        it.withTargets(
                            AppleCompileTarget.macosArm64,
                            AppleCompileTarget.iosArm64,
                        )
                    }
                }.withMinWatchOs(null)
                .withMinTvos(null)
                .withMinMacos(null)
                .withMinIos(null)
                .withRawDependencies(
                    KotlinSource.of(
                        content =
                            """
                            localPackage(
                                path = "${localPackageDirectory.absolutePath}",
                                products = {
                                    add("LocalSourceDummyFramework", exportToKotlin = true)
                                },
                            )
                            """.trimIndent(),
                    ),
                ).withKotlinSources(
                    KotlinSource.of(
                        imports = listOf("LocalSourceDummyFramework.LocalSourceDummy"),
                    ),
                ).withSwiftSources(
                    SwiftSource.of(
                        content =
                            """
                            import Foundation
                            import LocalSourceDummyFramework
                            @objc public class MySwiftDummyClass: NSObject {
                            }
                            """.trimIndent(),
                    ),
                ).build()

        // When
        val result =
            GradleBuilder
                .runner(fixture.gradleProject.rootDir, "build")
                .build()

        // Then
        assertThat(result).task(":library:build").succeeded()
    }
}
