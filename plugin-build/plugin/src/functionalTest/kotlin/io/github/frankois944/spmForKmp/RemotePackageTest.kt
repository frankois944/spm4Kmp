package io.github.frankois944.spmForKmp

import com.autonomousapps.kit.GradleBuilder
import com.autonomousapps.kit.truth.TestKitTruth.Companion.assertThat
import io.github.frankois944.spmForKmp.config.AppleCompileTarget
import io.github.frankois944.spmForKmp.fixture.KotlinSource
import io.github.frankois944.spmForKmp.fixture.SmpKMPTestFixture
import io.github.frankois944.spmForKmp.fixture.SwiftSource
import io.github.frankois944.spmForKmp.utils.BaseTest
import org.junit.jupiter.api.Test

class RemotePackageTest : BaseTest() {
    @Test
    fun `build with remote packages by version`() {
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
    url = URI("https://github.com/krzyzanowskim/CryptoSwift.git"),
    version = "1.8.3",
    products = {
        add("CryptoSwift")
    },
)
                            """.trimIndent(),
                    ),
                ).withKotlinSources(
                    KotlinSource.of(
                        imports = listOf("dummy.MySwiftClass"),
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

        val result =
            GradleBuilder
                .runner(fixture.gradleProject.rootDir, "build")
                .build()

        // Then
        assertThat(result).task(":library:build").succeeded()
    }

    @Test
    fun `build with remote packages by branch`() {
        // Given
        val fixture =
            SmpKMPTestFixture
                .builder()
                .withBuildPath(testProjectDir.root.absolutePath)
                .withRawDependencies(
                    KotlinSource.of(
                        content =
                            """
remotePackageBranch(
   url = URI("https://github.com/krzyzanowskim/CryptoSwift.git"),
   branch = "main",
   products = {
       add("CryptoSwift")
   },
)
                            """.trimIndent(),
                    ),
                ).withKotlinSources(
                    KotlinSource.of(
                        imports = listOf("dummy.MySwiftClass"),
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

        val result =
            GradleBuilder
                .runner(fixture.gradleProject.rootDir, "build")
                .build()

        // Then
        assertThat(result).task(":library:build").succeeded()
    }

    @Test
    fun `build with remote packages by commit`() {
        // Given
        val fixture =
            SmpKMPTestFixture
                .builder()
                .withBuildPath(testProjectDir.root.absolutePath)
                .withRawDependencies(
                    KotlinSource.of(
                        content =
                            """
remotePackageCommit(
   url = URI("https://github.com/krzyzanowskim/CryptoSwift.git"),
   revision = "729e01bc9b9dab466ac85f21fb9ee2bc1c61b258",
   products = {
       add("CryptoSwift")
   },
)
                            """.trimIndent(),
                    ),
                ).withKotlinSources(
                    KotlinSource.of(
                        imports = listOf("dummy.MySwiftClass"),
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

        val result =
            GradleBuilder
                .runner(fixture.gradleProject.rootDir, "build")
                .build()

        // Then
        assertThat(result).task(":library:build").succeeded()
    }

    @Test
    fun `build with complex remote packages`() {
        // Given
        val fixture =
            SmpKMPTestFixture
                .builder()
                .withBuildPath(testProjectDir.root.absolutePath)
                .withTargets(AppleCompileTarget.iosSimulatorArm64, AppleCompileTarget.iosArm64)
                .withRawDependencies(
                    KotlinSource.of(
                        content =
                            """
remotePackageVersion(
    url = URI("https://github.com/firebase/firebase-ios-sdk.git"),
    version = "11.6.0",
    packageName = "",
    products = {
        add(
            ProductName("FirebaseCore"),
            ProductName("FirebaseAnalytics"),
            exportToKotlin = true,
        )
        add("FirebasePerformance", exportToKotlin = true)
        add("FirebaseCrashlytics")
    },
)
                            """.trimIndent(),
                    ),
                ).withKotlinSources(
                    KotlinSource.of(
                        imports =
                            listOf(
                                "FirebaseCore.FIRApp",
                                "FirebaseAnalytics.FIRConsentStatusGranted",
                                "FirebasePerformance.FIRPerformance",
                            ),
                    ),
                ).withSwiftSources(
                    SwiftSource.of(
                        content =
                            """
                            import Foundation
                            import FirebaseCore
                            import FirebaseAnalytics
                            import FirebaseCrashlytics
                            import FirebasePerformance

                            @objc public class MySwiftClass: NSObject {
                            }
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
