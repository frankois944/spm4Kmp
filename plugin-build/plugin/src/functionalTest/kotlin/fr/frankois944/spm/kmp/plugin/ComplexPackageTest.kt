package fr.frankois944.spm.kmp.plugin

import com.autonomousapps.kit.GradleBuilder
import com.autonomousapps.kit.truth.TestKitTruth.Companion.assertThat
import fr.frankois944.spm.kmp.plugin.definition.SwiftDependency
import fr.frankois944.spm.kmp.plugin.fixture.KotlinSource
import fr.frankois944.spm.kmp.plugin.fixture.SmpKMPTestFixture
import fr.frankois944.spm.kmp.plugin.fixture.SwiftSource
import fr.frankois944.spm.kmp.plugin.utils.OpenFolderOnFailureExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.io.File

class ComplexPackageTest {
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
    fun `build with multiple packages type`() {
        val localPackageDirectory = File("src/functionalTest/resources/LocalDummyFramework")
        val xcFrameworkDirectory = File("src/functionalTest/resources/DummyFramework.xcframework")
        // Given
        val fixture =
            SmpKMPTestFixture
                .builder()
                .withTargets(CompileTarget.iosSimulatorArm64, CompileTarget.iosX64, CompileTarget.iosArm64)
                .withDependencies(
                    buildList {
                        add(
                            SwiftDependency.Package.Remote.Version(
                                url = "https://github.com/krzyzanowskim/CryptoSwift.git",
                                names = listOf("CryptoSwift"),
                                version = "1.8.3",
                            ),
                        )
                        add(
                            SwiftDependency.Package.Remote.Branch(
                                url = "https://github.com/kishikawakatsumi/KeychainAccess.git",
                                names = listOf("KeychainAccess"),
                                branch = "master",
                            ),
                        )
                        add(
                            SwiftDependency.Package.Remote.Commit(
                                url = "https://github.com/square/Valet",
                                names = listOf("Valet"),
                                revision = "0a928c410378db708dc4b9d1cf1c1cb0cb75621f",
                            ),
                        )
                        add(
                            SwiftDependency.Package.Local(
                                path = localPackageDirectory.absolutePath,
                                packageName = "LocalDummyFramework",
                            ),
                        )
                        add(
                            SwiftDependency.Binary.Local(
                                path = xcFrameworkDirectory.absolutePath,
                                packageName = "DummyFramework",
                            ),
                        )
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
                            import CryptoSwift.SWIFT_TYPEDEFS
                            import dummy.MySwiftClassEmbedded
                            import LocalDummyFramework.MySwiftClass

                            """.trimIndent(),
                    ),
                ).withSwiftSources(
                    SwiftSource.of(
                        content =
                            """
                            import Foundation
                            import CryptoSwift
                            import KeychainAccess
                            import Valet
                            import DummyFramework
                            import LocalDummyFramework
                            import FirebaseCore
                            import FirebaseAnalytics
                            import FirebaseCrashlytics

                            @objc public class MySwiftClassEmbedded: NSObject {
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
            GradleBuilder.build(project, "build")

        // Then
        assertThat(result).task(":library:build").succeeded()
    }
}
