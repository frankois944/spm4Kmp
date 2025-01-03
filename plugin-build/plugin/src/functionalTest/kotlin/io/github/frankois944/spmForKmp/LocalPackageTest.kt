package io.github.frankois944.spmForKmp

import com.autonomousapps.kit.GradleBuilder
import com.autonomousapps.kit.truth.TestKitTruth.Companion.assertThat
import io.github.frankois944.spmForKmp.definition.SwiftDependency
import io.github.frankois944.spmForKmp.fixture.KotlinSource
import io.github.frankois944.spmForKmp.fixture.SmpKMPTestFixture
import io.github.frankois944.spmForKmp.fixture.SwiftSource
import io.github.frankois944.spmForKmp.utils.OpenFolderOnFailureExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.io.File

class LocalPackageTest {
    private var folderTopOpen: String? = null

    @RegisterExtension
    @JvmField
    val openFolderOnFailure =
        OpenFolderOnFailureExtension {
            folderTopOpen ?: ""
        }

    @BeforeEach
    fun beforeEach() {
        folderTopOpen = null
    }

    @Test
    fun `build with local packages`() {
        val localPackageDirectory = File("src/functionalTest/resources/LocalDummyFramework")
        // Given
        val fixture =
            SmpKMPTestFixture
                .builder()
                .withTargets(
                    CompileTarget.iosArm64,
                    CompileTarget.iosX64,
                    CompileTarget.iosSimulatorArm64,
                    CompileTarget.macosArm64,
                    CompileTarget.macosX64,
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

        val project = fixture.gradleProject.rootDir
        folderTopOpen = project.absolutePath
        // When
        val result = GradleBuilder.build(project, "build")

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
                .withTargets(CompileTarget.iosSimulatorArm64, CompileTarget.macosArm64, CompileTarget.iosArm64)
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

        val project = fixture.gradleProject.rootDir
        folderTopOpen = project.absolutePath
        // When
        val result = GradleBuilder.build(project, "build")

        // Then
        assertThat(result).task(":library:build").succeeded()
    }
}
