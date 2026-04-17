package io.github.frankois944.spmForKmp

import com.autonomousapps.kit.GradleBuilder
import com.autonomousapps.kit.truth.TestKitTruth.Companion.assertThat
import io.github.frankois944.spmForKmp.config.AppleCompileTarget
import io.github.frankois944.spmForKmp.fixture.KotlinSource
import io.github.frankois944.spmForKmp.fixture.SmpKMPTestFixture
import io.github.frankois944.spmForKmp.fixture.SwiftSource
import io.github.frankois944.spmForKmp.utils.BaseTest
import org.junit.jupiter.api.Test

class MultiTargetBinaryPackageTest : BaseTest() {
    @Test
    fun `build with remote binary packages and multiple targets`() {
        // Given
        val fixture =
            SmpKMPTestFixture
                .builder()
                .withBuildPath(testProjectDir.root.absolutePath)
                .withTargets(
                    AppleCompileTarget.iosSimulatorArm64,
                    AppleCompileTarget.iosArm64,
                ).withRawDependencies(
                    KotlinSource.of(
                        content =
                            """
                            remoteBinary(
                                url = URI("https://spmforkmp.eu/DummyFrameworkV2.xcframework.zip"),
                                checksum = "ce79ee72991d40620f156e407e47145ba3279cc15ebf20bb2560c7882d69e56e",
                                packageName = "DummyFramework",
                                exportToKotlin = true
                            )
                            remoteBinary(
                                url = uri("https://github.com/wanliyunyan/LibXray/releases/download/25.8.3/LibXray.xcframework.zip"),
                                packageName = "LibXray",
                                exportToKotlin = true,
                                checksum = "1f478c370bbe9cce12ed7f2f6101853a74c3a2220253dbaf5abcc446260c393d",
                            )
                            """.trimIndent(),
                    ),
                ).withKotlinSources(
                    KotlinSource.of(
                        imports = listOf("DummyFramework.DummyFrameworkVersionNumber"),
                    ),
                ).withSwiftSources(
                    SwiftSource.of(
                        content =
                            """
                            import Foundation
                            import DummyFramework
                            import LibXray
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
