package io.github.frankois944.spmForKmp

import com.autonomousapps.kit.GradleBuilder
import com.autonomousapps.kit.truth.TestKitTruth.Companion.assertThat
import io.github.frankois944.spmForKmp.config.CompileTarget
import io.github.frankois944.spmForKmp.definition.SwiftDependency
import io.github.frankois944.spmForKmp.definition.product.ProductName
import io.github.frankois944.spmForKmp.fixture.KotlinSource
import io.github.frankois944.spmForKmp.fixture.SmpKMPTestFixture
import io.github.frankois944.spmForKmp.fixture.SwiftSource
import io.github.frankois944.spmForKmp.utils.BaseTest
import org.junit.jupiter.api.Test
import java.io.File
import java.net.URI

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
                .withTargets(CompileTarget.iosSimulatorArm64)
                .withDependencies(
                    buildList {
                        add(
                            SwiftDependency.Package.Remote.Version(
                                url = URI("https://github.com/krzyzanowskim/CryptoSwift.git"),
                                packageName = "",
                                products = {
                                    add(
                                        ProductName("CryptoSwift"),
                                        exportToKotlin = false,
                                    )
                                },
                                version = "1.8.3",
                            ),
                        )
                        add(
                            SwiftDependency.Package.Remote.Branch(
                                url = URI("https://github.com/kishikawakatsumi/KeychainAccess.git"),
                                products = {
                                    add("KeychainAccess")
                                },
                                branch = "master",
                            ),
                        )
                        add(
                            SwiftDependency.Package.Remote.Branch(
                                url = URI("https://github.com/FluidGroup/JAYSON"),
                                products = {
                                    add("JAYSON", exportToKotlin = true)
                                },
                                branch = "main",
                            ),
                        )
                        add(
                            SwiftDependency.Package.Remote.Commit(
                                url = URI("https://github.com/square/Valet"),
                                products = {
                                    add("Valet")
                                },
                                revision = "e900692d551b1986fc80aa3968d40e7af3b1e858",
                            ),
                        )
                        add(
                            SwiftDependency.Package.Remote.Commit(
                                url = URI("https://github.com/venmo/Static"),
                                products = {
                                    add("Static", exportToKotlin = true)
                                },
                                revision = "622a6804d39515600ead16e6259cb5d5e50f40df",
                            ),
                        )
                        add(
                            SwiftDependency.Package.Local(
                                path = localPackageDirectory.absolutePath,
                                packageName = "LocalSourceDummyFramework",
                                products = {
                                    add("LocalSourceDummyFramework", exportToKotlin = true)
                                },
                            ),
                        )
                        add(
                            SwiftDependency.Binary.Local(
                                path = xcFrameworkDirectory.absolutePath,
                                packageName = "DummyFramework",
                                exportToKotlin = true,
                            ),
                        )
                        add(
                            SwiftDependency.Package.Remote.Version(
                                url = URI("https://github.com/firebase/firebase-ios-sdk.git"),
                                products = {
                                    add("FirebaseCore", "FirebaseAnalytics", "FirebaseCrashlytics")
                                },
                                version = "11.6.0",
                                packageName = "firebase-ios-sdk",
                            ),
                        )
                    },
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
