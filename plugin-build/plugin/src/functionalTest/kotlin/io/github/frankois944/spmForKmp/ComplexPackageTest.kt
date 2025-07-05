package io.github.frankois944.spmForKmp

import com.autonomousapps.kit.GradleBuilder
import com.autonomousapps.kit.truth.TestKitTruth.Companion.assertThat
import io.github.frankois944.spmForKmp.config.AppleCompileTarget
import io.github.frankois944.spmForKmp.fixture.KotlinSource
import io.github.frankois944.spmForKmp.fixture.SmpKMPTestFixture
import io.github.frankois944.spmForKmp.fixture.SwiftSource
import io.github.frankois944.spmForKmp.utils.BaseTest
import org.junit.jupiter.api.Test
import java.io.File

class ComplexPackageTest : BaseTest() {
    @Test
    fun `build with multiple packages type`() {
        val localPackageDirectory = File("src/functionalTest/resources/LocalSourceDummyFramework")
        val xcFrameworkDirectory = File("src/functionalTest/resources/DummyFramework.xcframework")
        // Given
        val fixture =
            SmpKMPTestFixture
                .builder()
                .withBuildPath(testProjectDir.root.absolutePath)
                .withTargets(AppleCompileTarget.iosSimulatorArm64)
                .withRawDependencies(
                    KotlinSource.of(
                        content =
                            """
                            remotePackageVersion(
                                url = URI("https://github.com/krzyzanowskim/CryptoSwift.git"),
                                packageName = "",
                                products = {
                                    add(
                                        ProductName("CryptoSwift"),
                                        exportToKotlin = false,
                                    )
                                },
                                version = "1.8.3",
                            )
                            remotePackageBranch(
                                url = URI("https://github.com/kishikawakatsumi/KeychainAccess.git"),
                                products = {
                                    add("KeychainAccess")
                                },
                                branch = "master",
                            )
                            remotePackageBranch(
                                url = URI("https://github.com/FluidGroup/JAYSON"),
                                products = {
                                    add("JAYSON", exportToKotlin = true)
                                },
                                branch = "main",
                            )
                            remotePackageCommit(
                                url = URI("https://github.com/square/Valet"),
                                products = {
                                    add("Valet")
                                },
                                revision = "e900692d551b1986fc80aa3968d40e7af3b1e858",
                            )
                            remotePackageCommit(
                                url = URI("https://github.com/venmo/Static"),
                                products = {
                                    add("Static", exportToKotlin = true)
                                },
                                revision = "622a6804d39515600ead16e6259cb5d5e50f40df",
                            )
                            localPackage(
                                path = "${localPackageDirectory.absolutePath}",
                                packageName = "LocalSourceDummyFramework",
                                products = {
                                    add("LocalSourceDummyFramework", exportToKotlin = true)
                                },
                            )
                            localBinary(
                                path = "${xcFrameworkDirectory.absolutePath}",
                                packageName = "DummyFramework",
                                exportToKotlin = true,
                            )
                            remotePackageVersion(
                                url = URI("https://github.com/firebase/firebase-ios-sdk.git"),
                                products = {
                                    add("FirebaseCore", "FirebaseAnalytics", "FirebaseCrashlytics")
                                },
                                version = "11.6.0",
                                packageName = "firebase-ios-sdk",
                            )
                            """.trimIndent(),
                    ),
                ).withKotlinSources(
                    KotlinSource.of(
                        imports = listOf("dummy.MySwiftClassEmbedded", "LocalSourceDummyFramework.LocalSourceDummy"),
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
                            import LocalSourceDummyFramework
                            import FirebaseCore
                            import FirebaseAnalytics
                            import FirebaseCrashlytics
                            import Static
                            import JAYSON

                            @objc public class MySwiftClassEmbedded: NSObject {
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
}
