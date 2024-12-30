package fr.frankois944.spm.kmp.plugin

import com.autonomousapps.kit.GradleBuilder.build
import com.autonomousapps.kit.truth.TestKitTruth.Companion.assertThat
import fr.frankois944.spm.kmp.plugin.definition.SwiftPackageDependencyDefinition
import fr.frankois944.spm.kmp.plugin.fixture.KotlinSource
import fr.frankois944.spm.kmp.plugin.fixture.SmpKMPTestFixture
import fr.frankois944.spm.kmp.plugin.fixture.SwiftSource
import fr.frankois944.spm.kmp.plugin.utils.OpenFolderOnFailureExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.io.File

class BinaryModuleTest {
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
                .withDependencies(
                    buildList {
                        add(
                            SwiftPackageDependencyDefinition.RemoteBinary(
                                url = "https://raw.githubusercontent.com/frankois944/swift-klib-plugin/refs/heads/swift-spm-local-remote-lib/plugin/src/functionalTest/resources/DummyFramework.xcframework.zip",
                                checksum = "20f6264c95e80b6e2da7d2c9b9abe44b4426dac799927ea49fb7a4982f1affdb",
                                packageName = "DummyFramework",
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
    fun `build with local binaru packages`() {
        val xcframeworkDirectory = File("src/functionalTest/resources/DummyFramework.xcframework")
        // Given
        val fixture =
            SmpKMPTestFixture
                .builder()
                .withDependencies(
                    buildList {
                        add(
                            SwiftPackageDependencyDefinition.LocalBinary(
                                path = xcframeworkDirectory.absolutePath,
                                packageName = "DummyFramework",
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
