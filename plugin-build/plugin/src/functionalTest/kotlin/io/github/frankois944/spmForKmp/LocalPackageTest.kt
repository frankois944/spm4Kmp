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

class LocalPackageTest : BaseTest() {
    @Test
    fun `build with local packages`() {
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
                            AppleCompileTarget.watchosArm64,
                            AppleCompileTarget.watchosSimulatorArm64,
                            AppleCompileTarget.tvosArm64,
                            AppleCompileTarget.tvosSimulatorArm64,
                            AppleCompileTarget.iosArm64,
                        )
                    } else {
                        it.withTargets(
                            AppleCompileTarget.macosArm64,
                        )
                    }
                }.withRawDependencies(
                    KotlinSource.of(
                        content =
                            """
localPackage(
    path = ${localPackageDirectory.absolutePath},
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

    @Test
    fun `build with raw config`() {
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
                            AppleCompileTarget.watchosArm64,
                            AppleCompileTarget.watchosSimulatorArm64,
                            AppleCompileTarget.tvosArm64,
                            AppleCompileTarget.tvosSimulatorArm64,
                            AppleCompileTarget.iosArm64,
                        )
                    } else {
                        it.withTargets(
                            AppleCompileTarget.macosArm64,
                        )
                    }
                }.withRawDependencies(
                    KotlinSource.of(
                        content =
                            """
                            localBinary(
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

    @Test
    fun `build with local packages and no swift code`() {
        val localPackageDirectory = File("src/functionalTest/resources/LocalSourceDummyFramework")
        // Given
        val fixture =
            SmpKMPTestFixture
                .builder()
                .withBuildPath(testProjectDir.root.absolutePath)
                .withTargets(AppleCompileTarget.iosSimulatorArm64)
                .withRawDependencies(
                    KotlinSource.of(
                        content =
                            """
localBinary(
    path = ${localPackageDirectory.absolutePath},
    packageName = "LocalSourceDummyFramework",
    products = {
        add("LocalSourceDummyFramework", exportToKotlin = true)
    },
),
                            """.trimIndent(),
                    ),
                ).withKotlinSources(
                    KotlinSource.of(
                        imports = listOf("LocalSourceDummyFramework.LocalSourceDummy"),
                    ),
                ).build()

        val result =
            GradleBuilder
                .runner(fixture.gradleProject.rootDir, "build")
                .build()

        // Then
        assertThat(result).task(":library:build").succeeded()
    }

    @Test
    fun `build with local packages and custom depenency prefix`() {
        val localPackageDirectory = File("src/functionalTest/resources/LocalSourceDummyFramework")
        // Given
        val fixture =
            SmpKMPTestFixture
                .builder()
                .withBuildPath(testProjectDir.root.absolutePath)
                .withPackageDependencyPrefix("custom")
                .withRawDependencies(
                    KotlinSource.of(
                        content =
                            """
localPackage(
  path = ${localPackageDirectory.absolutePath},
  packageName = "LocalSourceDummyFramework",
  products = {
      add("LocalSourceDummyFramework", exportToKotlin = true)
  },
)
                            """.trimIndent(),
                    ),
                ).withKotlinSources(
                    KotlinSource.of(
                        imports = listOf("custom.LocalSourceDummyFramework.LocalSourceDummy"),
                    ),
                ).build()

        val result =
            GradleBuilder
                .runner(fixture.gradleProject.rootDir, "build")
                .build()

        // Then
        assertThat(result).task(":library:build").succeeded()
    }
}
