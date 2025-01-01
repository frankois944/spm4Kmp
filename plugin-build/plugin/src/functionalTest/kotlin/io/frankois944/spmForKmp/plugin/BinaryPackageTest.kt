package io.frankois944.spmForKmp.plugin

import com.autonomousapps.kit.GradleBuilder.build
import com.autonomousapps.kit.truth.TestKitTruth.Companion.assertThat
import io.frankois944.spmForKmp.plugin.definition.SwiftDependency
import io.frankois944.spmForKmp.plugin.fixture.KotlinSource
import io.frankois944.spmForKmp.plugin.fixture.SmpKMPTestFixture
import io.frankois944.spmForKmp.plugin.fixture.SwiftSource
import io.frankois944.spmForKmp.plugin.utils.OpenFolderOnFailureExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.io.File

class BinaryPackageTest {
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
    fun `build with remote binary packages`() {
        // Given
        val fixture =
            SmpKMPTestFixture
                .builder()
                .withTargets(CompileTarget.iosSimulatorArm64, CompileTarget.macosArm64)
                .withDependencies(
                    buildList {
                        add(
                            SwiftDependency.Binary.Remote(
                                url =
                                    "https://raw.githubusercontent.com/frankois944/spm4Kmp/refs/heads/main/" +
                                        "plugin-build/plugin/src/functionalTest/" +
                                        "resources/DummyFramework.xcframework.zip",
                                checksum = "20f6264c95e80b6e2da7d2c9b9abe44b4426dac799927ea49fb7a4982f1affdb",
                                packageName = "DummyFramework",
                                exportToKotlin = true,
                            ),
                        )
                    },
                ).withKotlinSources(
                    KotlinSource.of(
                        content =
                            """
                            package com.example
                            import DummyFramework.DummyFrameworkVersionNumber
                            """.trimIndent(),
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

        val project = fixture.gradleProject.rootDir
        folderTopOpen = project.absolutePath
        // When
        val result = build(project, "build")

        // Then
        assertThat(result).task(":library:build").succeeded()
    }

    @Test
    fun `build with local binaru package`() {
        val xcFrameworkDirectory = File("src/functionalTest/resources/DummyFramework.xcframework")
        // Given
        val fixture =
            SmpKMPTestFixture
                .builder()
                .withDependencies(
                    buildList {
                        add(
                            SwiftDependency.Binary.Local(
                                path = xcFrameworkDirectory.absolutePath,
                                packageName = "DummyFramework",
                                exportToKotlin = true,
                            ),
                        )
                    },
                ).withKotlinSources(
                    KotlinSource.of(
                        content =
                            """
                            package com.example
                            import DummyFramework.DummyFrameworkVersionNumber
                            """.trimIndent(),
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

        val project = fixture.gradleProject.rootDir
        folderTopOpen = project.absolutePath
        // When
        val result = build(project, "build")

        // Then
        assertThat(result).task(":library:build").succeeded()
    }
}
