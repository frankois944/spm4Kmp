package io.github.frankois944.spmForKmp

import com.autonomousapps.kit.GradleBuilder
import com.autonomousapps.kit.truth.TestKitTruth.Companion.assertThat
import io.github.frankois944.spmForKmp.fixture.KotlinSource
import io.github.frankois944.spmForKmp.fixture.SmpKMPTestFixture
import io.github.frankois944.spmForKmp.fixture.SwiftSource
import io.github.frankois944.spmForKmp.utils.BaseTest
import org.junit.jupiter.api.Test
import java.io.File

class BinaryPackageTest : BaseTest() {
    @Test
    fun `build with remote binary packages`() {
        // Given
        val fixture =
            SmpKMPTestFixture
                .builder()
                .withBuildPath(testProjectDir.root.absolutePath)
                .withRawDependencies(
                    KotlinSource.of(
                        content =
                            """
remoteBinary(
    url = URI("https://raw.githubusercontent.com/frankois944/spm4Kmp/refs/heads/main/plugin-build/plugin/src/functionalTest/resources/DummyFrameworkV2.xcframework.zip"),
    checksum = "ce79ee72991d40620f156e407e47145ba3279cc15ebf20bb2560c7882d69e56e",
    packageName = "DummyFramework",
    exportToKotlin = true
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

    @Test
    fun `build with local binary package`() {
        val xcFrameworkDirectory = File("src/functionalTest/resources/DummyFramework.xcframework")
        // Given
        val fixture =
            SmpKMPTestFixture
                .builder()
                .withBuildPath(testProjectDir.root.absolutePath)
                .withRawDependencies(
                    KotlinSource.of(
                        content =
                            """
localBinary(
    path = "${xcFrameworkDirectory.absolutePath}",
    packageName = "DummyFramework",
    exportToKotlin = true,
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
