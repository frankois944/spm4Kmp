package io.github.frankois944.spmForKmp

import com.autonomousapps.kit.GradleBuilder
import com.autonomousapps.kit.truth.TestKitTruth.Companion.assertThat
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
                .withRawDependencies(
                    KotlinSource.of(
                        content =
                            """
                            remotePackageVersion(
                               url = URI("https://github.com/google/GoogleSignIn-iOS"),
                               products = {
                                   add("GoogleSignIn", exportToKotlin = true)
                               },
                               version = "9.0.0",
                            )
                            """.trimIndent(),
                    ),
                ).withKotlinSources(
                    KotlinSource.of(
                        imports = listOf("GoogleSignIn.GIDSignIn"),
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
                .withRawDependencies(
                    KotlinSource.of(
                        content =
                            """
                            remotePackageVersion(
                                url = URI("https://github.com/firebase/firebase-ios-sdk.git"),
                                products = {
                                    add(
                                        ProductName(
                                            name = "FirebaseAppDistribution",
                                            alias = "FirebaseAppDistribution-Beta",
                                        ),
                                        exportToKotlin = true,
                                    )
                                    add(
                                        ProductName(
                                            "FirebaseStorage",
                                        ),
                                        exportToKotlin = true,
                                    )
                                },
                                version = "11.6.0",
                            )
                            """.trimIndent(),
                    ),
                ).withKotlinSources(
                    KotlinSource.of(
                        imports = listOf("FirebaseAppDistribution.FIRAppDistribution", "FirebaseStorage.FIRStorage"),
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

    @Test
    fun `build with custom public header`() {
        val fixture =
            SmpKMPTestFixture
                .builder()
                .withBuildPath(testProjectDir.root.absolutePath)
                .withRawDependencies(
                    KotlinSource.of(
                        content =
                            """
                            remotePackageVersion(
                                url = URI("https://github.com/bugsnag/bugsnag-cocoa"),
                                products = {
                                    add(
                                        "Bugsnag",
                                        "BugsnagNetworkRequestPlugin",
                                        exportToKotlin = true,
                                    )
                                },
                                version = "6.31.0",
                            )
                            remotePackageVersion(
                                url = URI("https://github.com/bugsnag/bugsnag-cocoa-performance"),
                                version = "1.11.2",
                                products = {
                                    // Can be only used in your "src/swift" code.
                                    add("BugsnagPerformance", exportToKotlin = true)
                                },
                            )
                            """.trimIndent(),
                    ),
                ).withSwiftSources(
                    SwiftSource.of(
                        content =
                            """
                            import Foundation
                            import Bugsnag
                            import BugsnagNetworkRequestPlugin
                            import BugsnagPerformance
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
