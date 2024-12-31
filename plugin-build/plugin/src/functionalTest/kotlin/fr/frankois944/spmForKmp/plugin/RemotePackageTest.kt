package fr.frankois944.spmForKmp.plugin

import com.autonomousapps.kit.GradleBuilder.build
import com.autonomousapps.kit.truth.TestKitTruth.Companion.assertThat
import fr.frankois944.spmForKmp.plugin.definition.SwiftDependency
import fr.frankois944.spmForKmp.plugin.fixture.KotlinSource
import fr.frankois944.spmForKmp.plugin.fixture.SmpKMPTestFixture
import fr.frankois944.spmForKmp.plugin.fixture.SwiftSource
import fr.frankois944.spmForKmp.plugin.utils.OpenFolderOnFailureExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class RemotePackageTest {
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
    fun `build with remote packages by version`() {
        // Given
        val fixture =
            SmpKMPTestFixture
                .builder()
                .withDependencies(
                    buildList {
                        add(
                            SwiftDependency.Package.Remote.Version(
                                url = "https://github.com/krzyzanowskim/CryptoSwift.git",
                                names = listOf("CryptoSwift"),
                                version = "1.8.3",
                            ),
                        )
                    },
                ).withKotlinSources(
                    KotlinSource.of(
                        content =
                            """
                            package com.example
                            import CryptoSwift.SWIFT_TYPEDEFS
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
        val result =
            build(project, "build")

        // Then
        assertThat(result).task(":library:build").succeeded()
    }

    @Test
    fun `build with remote packages by branch`() {
        // Given
        val fixture =
            SmpKMPTestFixture
                .builder()
                .withDependencies(
                    buildList {
                        add(
                            SwiftDependency.Package.Remote.Branch(
                                url = "https://github.com/krzyzanowskim/CryptoSwift.git",
                                names = listOf("CryptoSwift"),
                                packageName = "CryptoSwift",
                                branch = "main",
                            ),
                        )
                    },
                ).withKotlinSources(
                    KotlinSource.of(
                        content =
                            """
                            package com.example
                            import CryptoSwift.SWIFT_TYPEDEFS
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
        val result =
            build(project, "build")

        // Then
        assertThat(result).task(":library:build").succeeded()
    }

    @Test
    fun `build with remote packages by commit`() {
        // Given
        val fixture =
            SmpKMPTestFixture
                .builder()
                .withDependencies(
                    buildList {
                        add(
                            SwiftDependency.Package.Remote.Commit(
                                url = "https://github.com/krzyzanowskim/CryptoSwift.git",
                                names = listOf("CryptoSwift"),
                                revision = "729e01bc9b9dab466ac85f21fb9ee2bc1c61b258",
                            ),
                        )
                    },
                ).withKotlinSources(
                    KotlinSource.of(
                        content =
                            """
                            package com.example
                            import CryptoSwift.SWIFT_TYPEDEFS
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
        val result =
            build(project, "build")

        // Then
        assertThat(result).task(":library:build").succeeded()
    }

    @Test
    fun `build with complex remote packages`() {
        // Given
        val fixture =
            SmpKMPTestFixture
                .builder()
                .withTargets(CompileTarget.iosSimulatorArm64, CompileTarget.iosX64, CompileTarget.iosArm64)
                .withDependencies(
                    buildList {
                        add(
                            SwiftDependency.Package.Remote.Version(
                                url = "https://github.com/firebase/firebase-ios-sdk.git",
                                names = listOf("FirebaseCore", "FirebaseAnalytics", "FirebaseCrashlytics"),
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
                            import FirebaseCore.FIRApp
                            import FirebaseCrashlytics.FIRCrashlyticsMeta
                            import FirebaseAnalytics.FIRConsentStatusGranted
                            """.trimIndent(),
                    ),
                ).withSwiftSources(
                    SwiftSource.of(
                        content =
                            """
                            import Foundation
                            import FirebaseCore
                            import FirebaseAnalytics
                            import FirebaseCrashlytics

                            @objc public class MySwiftClass: NSObject {
                            }
                            """.trimIndent(),
                    ),
                ).build()

        val project = fixture.gradleProject.rootDir
        folderTopOpen = project.absolutePath
        // When
        val result =
            build(project, "build")

        // Then
        assertThat(result).task(":library:build").succeeded()
    }
}
