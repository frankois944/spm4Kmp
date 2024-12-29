package fr.frankois944.spm.kmp.plugin

import com.autonomousapps.kit.GradleBuilder.build
import com.autonomousapps.kit.truth.TestKitTruth.Companion.assertThat
import fr.frankois944.spm.kmp.plugin.definition.SwiftPackageDependencyDefinition
import fr.frankois944.spm.kmp.plugin.fixture.KotlinSource
import fr.frankois944.spm.kmp.plugin.fixture.SmpKMPTestFixture
import fr.frankois944.spm.kmp.plugin.fixture.SwiftSource
import fr.frankois944.spm.kmp.plugin.utils.OpenFolderOnFailureExtension
import fr.frankois944.spm.kmp.plugin.utils.assertPackageResolved
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class BasicModuleTest {
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

    fun onFailed() {
    }

    @Test
    fun `build simple remote package`() {
        // Given
        val fixture =
            SmpKMPTestFixture
                .builder()
                .withDependencies(
                    buildList {
                        add(
                            SwiftPackageDependencyDefinition.RemoteDefinition.Version(
                                url = "https://github.com/krzyzanowskim/CryptoSwift.git",
                                names = listOf("CryptoSwift"),
                                version = "1.8.3",
                            ),
                        )
                        add(
                            SwiftPackageDependencyDefinition.RemoteDefinition.Version(
                                url = "https://github.com/firebase/firebase-ios-sdk.git",
                                names = listOf("FirebaseCore", "FirebaseAnalytics"),
                                version = "11.6.0",
                                packageName = "firebase-ios-sdk",
                            ),
                        )
                    },
                ).withKotlinSources(
                    KotlinSource.of(
                        content =
                            """
                            package com.example
                            import CryptoSwift.SWIFT_TYPEDEF
                            import dummy.MySwiftClass
                            """.trimIndent(),
                    ),
                ).withSwiftSources(
                    SwiftSource.of(
                        content =
                            """
                            import Foundation
                            import CryptoSwift
                            @objc public class MySwiftClass: NSObject {
                                @objc public func toMD5(value: String) -> String {
                                    return value.md5()
                                }
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
        assertPackageResolved(fixture, "CryptoSwift")
    }
}
