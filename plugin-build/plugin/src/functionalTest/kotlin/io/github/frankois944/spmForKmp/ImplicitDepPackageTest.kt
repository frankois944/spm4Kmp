package io.github.frankois944.spmForKmp

import com.autonomousapps.kit.GradleBuilder
import com.autonomousapps.kit.truth.TestKitTruth.Companion.assertThat
import io.github.frankois944.spmForKmp.definition.SwiftDependency
import io.github.frankois944.spmForKmp.fixture.KotlinSource
import io.github.frankois944.spmForKmp.fixture.SmpKMPTestFixture
import io.github.frankois944.spmForKmp.fixture.SwiftSource
import io.github.frankois944.spmForKmp.utils.BaseTest
import org.junit.jupiter.api.Test

class ImplicitDepPackageTest : BaseTest() {
    @Test
    fun `build with implicit package dependency`() {
        // Given
        val fixture =
            SmpKMPTestFixture
                .builder()
                .withBuildPath(testProjectDir.root.absolutePath)
                .withDependencies(
                    buildList {
                        add(
                            SwiftDependency.Package.Remote.Version(
                                url = "https://github.com/google/GoogleSignIn-iOS",
                                names = listOf("GoogleSignIn"),
                                packageName = "GoogleSignIn-iOS",
                                version = "8.0.0",
                                exportToKotlin = true,
                            ),
                        )
                    },
                ).withKotlinSources(
                    KotlinSource.of(
                        content =
                            """
                            package com.example
                            import GoogleSignIn.GIDSignIn
                            """.trimIndent(),
                    ),
                ).withSwiftSources(
                    SwiftSource.of(
                        content =
                            """
                            import GoogleSignIn
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

    @Test
    fun `build and export complex firebase dependencies`() {
        // Given
        val fixture =
            SmpKMPTestFixture
                .builder()
                .withBuildPath(testProjectDir.root.absolutePath)
                .withDependencies(
                    buildList {
                        add(
                            SwiftDependency.Package.Remote.Version(
                                url = "https://github.com/firebase/firebase-ios-sdk.git",
                                names = listOf("FirebaseAppDistribution-Beta", "FirebaseStorage"),
                                packageName = "firebase-ios-sdk",
                                version = "11.6.0",
                                exportToKotlin = true,
                            ),
                        )
                    },
                ).withKotlinSources(
                    KotlinSource.of(
                        content =
                            """
                            package com.example
                            import `FirebaseAppDistribution-Beta`.FIRAppDistribution
                            import FirebaseStorage.FIRStorage
                            """.trimIndent(),
                    ),
                ).withSwiftSources(
                    SwiftSource.of(
                        content =
                            """
                            import Foundation
                            import FirebaseAppDistribution
                            import FirebaseStorage
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
