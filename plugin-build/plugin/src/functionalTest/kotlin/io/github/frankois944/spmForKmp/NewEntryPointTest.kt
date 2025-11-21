package io.github.frankois944.spmForKmp

import com.autonomousapps.kit.GradleBuilder
import com.autonomousapps.kit.truth.TestKitTruth.Companion.assertThat
import io.github.frankois944.spmForKmp.fixture.KotlinSource
import io.github.frankois944.spmForKmp.fixture.SmpKMPTestFixture
import io.github.frankois944.spmForKmp.fixture.SwiftSource
import io.github.frankois944.spmForKmp.utils.BaseTest
import org.junit.jupiter.api.Test

class NewEntryPointTest : BaseTest() {
    @Test
    fun `build with new configuration entry point`() {
        // Given
        val fixture =
            SmpKMPTestFixture
                .builder()
                .withBuildPath(testProjectDir.root.absolutePath)
                .withRawTargetBlock(
                    KotlinSource.of(
                        content =
                            """
                            it.swiftPackage(groupName = "nativeIosShared") {
                                minIos = "16.0"
                                dependency {
                                    remotePackageVersion(
                                        url = uri("https://github.com/firebase/firebase-ios-sdk.git"),
                                        version = "12.3.0",
                                        packageName = "firebase-ios-sdk",
                                        products = { add("FirebaseAnalytics") },
                                    )
                                }
                            }
                            """.trimIndent(),
                    ),
                ).withSwiftSources(
                    SwiftSource.of(
                        content =
                            """
                            import UIKit
                            import FirebaseAnalytics
                            @objc public class TestView: UIView {}
                            """.trimIndent(),
                    ),
                ).withKotlinSources(
                    KotlinSource.of(
                        imports = listOf("dummy.TestView"),
                        content =
                            """
                            @kotlinx.cinterop.ExperimentalForeignApi
                            val view = TestView()
                            """.trimIndent(),
                    ),
                ).build()

        // When
        val result =
            GradleBuilder
                .runner(fixture.gradleProject.rootDir, "build")
                .build()

        // Then
        assertThat(result).task(":library:build").succeeded()
    }
}
