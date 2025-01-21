package io.github.frankois944.spmForKmp

import com.autonomousapps.kit.GradleBuilder
import com.autonomousapps.kit.truth.TestKitTruth.Companion.assertThat
import io.github.frankois944.spmForKmp.definition.SwiftDependency
import io.github.frankois944.spmForKmp.definition.product.ProductName
import io.github.frankois944.spmForKmp.fixture.KotlinSource
import io.github.frankois944.spmForKmp.fixture.SmpKMPTestFixture
import io.github.frankois944.spmForKmp.fixture.SwiftSource
import io.github.frankois944.spmForKmp.utils.BaseTest
import org.junit.jupiter.api.Test
import java.net.URI

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
                                url = URI("https://github.com/google/GoogleSignIn-iOS"),
                                products = {
                                    add("GoogleSignIn", exportToKotlin = true)
                                },
                                version = "8.0.0",
                            ),
                        )
                    },
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
                .withDependencies(
                    buildList {
                        add(
                            SwiftDependency.Package.Remote.Version(
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
                            ),
                        )
                    },
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
}
