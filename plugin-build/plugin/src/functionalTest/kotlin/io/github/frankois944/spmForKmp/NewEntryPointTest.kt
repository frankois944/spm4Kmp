package io.github.frankois944.spmForKmp

import com.autonomousapps.kit.GradleBuilder
import com.autonomousapps.kit.truth.TestKitTruth.Companion.assertThat
import io.github.frankois944.spmForKmp.fixture.KotlinSource
import io.github.frankois944.spmForKmp.fixture.SmpKMPTestFixture
import io.github.frankois944.spmForKmp.fixture.SwiftSource
import io.github.frankois944.spmForKmp.utils.BaseTest
import org.junit.jupiter.api.Test
import java.io.File

class NewEntryPointTest : BaseTest() {
    @Test
    fun `build with new configuration entry point`() {
        val localPackageDirectory = File("src/functionalTest/resources/LocalSourceDummyFramework")
        // Given
        val fixture =
            SmpKMPTestFixture
                .builder()
                .withBuildPath(testProjectDir.root.absolutePath)
                .withRawTargetBlock(
                    KotlinSource.of(
                        content =
                            """
                            it.swiftPackageConfig(cinteropName = "nativeIosShared") {
                                minIos = "16.0"
                                dependency {
                                    localPackage(
                                        path = "${localPackageDirectory.absolutePath}",
                                        products = {
                                            add("LocalSourceDummyFramework")
                                        },
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
                            import LocalSourceDummyFramework
                            @objc public class TestView: UIView {}
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

    @Test
    fun `build with new configuration entry point with no cinteropName`() {
        val localPackageDirectory = File("src/functionalTest/resources/LocalSourceDummyFramework")
        // Given
        val fixture =
            SmpKMPTestFixture
                .builder()
                .withBuildPath(testProjectDir.root.absolutePath)
                .withRawTargetBlock(
                    KotlinSource.of(
                        content =
                            """
                            it.swiftPackageConfig {
                                minIos = "16.0"
                                dependency {
                                    localPackage(
                                        path = "${localPackageDirectory.absolutePath}",
                                        products = {
                                            add("LocalSourceDummyFramework")
                                        },
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
                            import LocalSourceDummyFramework
                            @objc public class TestView: UIView {}
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
