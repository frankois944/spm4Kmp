package io.github.frankois944.spmForKmp

import com.autonomousapps.kit.GradleBuilder
import com.autonomousapps.kit.truth.TestKitTruth.Companion.assertThat
import io.github.frankois944.spmForKmp.definition.ProductPackageConfig
import io.github.frankois944.spmForKmp.definition.SwiftDependency
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
                            CompileTarget.macosArm64,
                            CompileTarget.watchosArm64,
                            CompileTarget.watchosSimulatorArm64,
                            CompileTarget.tvosArm64,
                            CompileTarget.tvosSimulatorArm64,
                            CompileTarget.iosArm64,
                        )
                    } else {
                        it.withTargets(
                            CompileTarget.macosArm64,
                        )
                    }
                }.withDependencies(
                    buildList {
                        add(
                            SwiftDependency.Package.Local(
                                path = localPackageDirectory.absolutePath,
                                packageName = "",
                                products =
                                    listOf(
                                        ProductPackageConfig(
                                            "LocalSourceDummyFramework",
                                            exportToKotlin = true,
                                        ),
                                    ),
                            ),
                        )
                    },
                ).withKotlinSources(
                    KotlinSource.of(
                        content =
                            """
                            package com.example
                            import LocalSourceDummyFramework.LocalSourceDummy
                            """.trimIndent(),
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
                .withTargets(CompileTarget.iosSimulatorArm64)
                .withDependencies(
                    buildList {
                        add(
                            SwiftDependency.Package.Local(
                                path = localPackageDirectory.absolutePath,
                                packageName = "LocalSourceDummyFramework",
                                products =
                                    listOf(
                                        ProductPackageConfig(
                                            "LocalSourceDummyFramework",
                                            exportToKotlin = true,
                                        ),
                                    ),
                            ),
                        )
                    },
                ).withKotlinSources(
                    KotlinSource.of(
                        content =
                            """
                            package com.example
                            import LocalSourceDummyFramework.LocalSourceDummy
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
}
