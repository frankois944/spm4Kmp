package io.github.frankois944.spmForKmp

import com.autonomousapps.kit.GradleBuilder
import com.autonomousapps.kit.truth.TestKitTruth.Companion.assertThat
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
        val localPackageDirectory = File("src/functionalTest/resources/LocalDummyFramework")
        // Given
        val fixture =
            SmpKMPTestFixture
                .builder()
                .withBuildPath(testProjectDir.root.absolutePath)
                .withTargets(
                    CompileTarget.macosArm64,
                ).withDependencies(
                    buildList {
                        add(
                            SwiftDependency.Package.Local(
                                path = localPackageDirectory.absolutePath,
                                packageName = "LocalDummyFramework",
                                exportToKotlin = true,
                            ),
                        )
                    },
                ).withKotlinSources(
                    KotlinSource.of(
                        content =
                            """
                            package com.example
                            import LocalDummyFramework.MySwiftClass
                            """.trimIndent(),
                    ),
                ).withSwiftSources(
                    SwiftSource.of(
                        content =
                            """
                            import Foundation
                            import LocalDummyFramework
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
        val localPackageDirectory = File("src/functionalTest/resources/LocalDummyFramework")
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
                                packageName = "LocalDummyFramework",
                                exportToKotlin = true,
                            ),
                        )
                    },
                ).withKotlinSources(
                    KotlinSource.of(
                        content =
                            """
                            package com.example
                            import LocalDummyFramework.MySwiftClass
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
