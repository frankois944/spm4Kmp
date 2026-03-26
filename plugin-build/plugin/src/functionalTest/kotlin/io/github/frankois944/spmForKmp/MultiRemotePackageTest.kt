package io.github.frankois944.spmForKmp

import com.autonomousapps.kit.GradleBuilder
import com.autonomousapps.kit.truth.TestKitTruth.Companion.assertThat
import io.github.frankois944.spmForKmp.config.AppleCompileTarget
import io.github.frankois944.spmForKmp.fixture.KotlinSource
import io.github.frankois944.spmForKmp.fixture.SmpKMPTestFixture
import io.github.frankois944.spmForKmp.fixture.SwiftSource
import io.github.frankois944.spmForKmp.utils.BaseTest
import org.junit.jupiter.api.Test

class MultiRemotePackageTest : BaseTest() {
    @Test
    fun `build with multiple remote packages`() {
        // Given
        val fixture =
            SmpKMPTestFixture
                .builder()
                .withBuildPath(testProjectDir.root.absolutePath)
                .withTargets(AppleCompileTarget.iosSimulatorArm64, AppleCompileTarget.iosArm64)
                .withRawTargetBlock(
                    KotlinSource.of(
                        content =
                            """
                            it.swiftPackageConfig(cinteropName = "nativeAnalytics") {
                                minIos = "12.0"
                                dependency {
                                    remotePackageVersion(
                                        url = URI("https://github.com/firebase/firebase-ios-sdk.git"),
                                        version = "11.6.0",
                                        products = {
                                            add(
                                                ProductName("FirebaseCore"),
                                                ProductName("FirebaseAnalytics"),
                                                exportToKotlin = true,
                                            )
                                            add("FirebaseCrashlytics")
                                        },
                                    )
                                }
                            }
                            it.swiftPackageConfig(cinteropName = "nativePerformance") {
                                minIos = "12.0"
                                dependency {
                                    remotePackageVersion(
                                        url = URI("https://github.com/firebase/firebase-ios-sdk.git"),
                                        version = "11.6.0",
                                        products = {
                                            add("FirebasePerformance", exportToKotlin = true)                                        },
                                    )
                                }
                            }
                            """.trimIndent(),
                    ),
                ).withKotlinSources(
                    KotlinSource.of(
                        imports =
                            listOf(
                                "FirebaseCore.FIRApp",
                                "FirebaseAnalytics.FIRConsentStatusGranted",
                            ),
                    ),
                    KotlinSource.of(
                        imports =
                            listOf(
                                "FirebasePerformance.FIRPerformance",
                            ),
                    ),
                ).withSwiftSources(
                    SwiftSource.of(
                        cinteropName = "nativeAnalytics",
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
                    SwiftSource.of(
                        cinteropName = "nativePerformance",
                        content =
                            """
                            import Foundation
                            import FirebasePerformance

                            @objc public class MySwiftClass: NSObject {
                            }
                            """.trimIndent(),
                    )
                ).build()

        val result =
            GradleBuilder
                .runner(fixture.gradleProject.rootDir, "build")
                .build()

        // Then
        assertThat(result).task(":library:build").succeeded()
    }
}
