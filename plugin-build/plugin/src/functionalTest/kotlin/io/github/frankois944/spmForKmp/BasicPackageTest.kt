package io.github.frankois944.spmForKmp

import com.autonomousapps.kit.GradleBuilder
import com.autonomousapps.kit.truth.TestKitTruth.Companion.assertThat
import io.github.frankois944.spmForKmp.config.AppleCompileTarget
import io.github.frankois944.spmForKmp.definition.SwiftDependency
import io.github.frankois944.spmForKmp.definition.product.ProductName
import io.github.frankois944.spmForKmp.fixture.KotlinSource
import io.github.frankois944.spmForKmp.fixture.SmpKMPTestFixture
import io.github.frankois944.spmForKmp.fixture.SwiftSource
import io.github.frankois944.spmForKmp.utils.BaseTest
import org.junit.jupiter.api.Test
import java.io.File
import java.net.URI

class BasicPackageTest : BaseTest() {
    @Test
    fun `build with imported UIKit framework is successful`() {
        // Given
        val fixture =
            SmpKMPTestFixture
                .builder()
                .withBuildPath(testProjectDir.root.absolutePath)
                .withSwiftSources(
                    SwiftSource.of(
                        content =
                            """
                            import UIKit
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

    @Test
    fun `build with alternative declaration of dependency`() {
        val localPackageDirectory = File("src/functionalTest/resources/LocalSourceDummyFramework")
        // Given
        val fixture =
            SmpKMPTestFixture
                .builder()
                .withBuildPath(testProjectDir.root.absolutePath)
                .withRawDependencies(
                    KotlinSource.of(
                        content =
                            """
                            SwiftDependency.Package.Local(
                                path = "${localPackageDirectory.absolutePath}",
                                packageName = "LocalSourceDummyFramework",
                                products = {
                                    add("LocalSourceDummyFramework", exportToKotlin = true)
                                },
                            )
                            """.trimIndent(),
                    ),
                    KotlinSource.of(
                        content =
                            """
                            SwiftDependency.Package.Remote.Version(
                                url = URI("https://github.com/krzyzanowskim/CryptoSwift.git"),
                                version = "1.8.3",
                                products = {
                                    add("CryptoSwift")
                                },
                            )
                            """.trimIndent(),
                    ),
                ).withSwiftSources(
                    SwiftSource.of(
                        content =
                            """
                            import UIKit
                            import LocalSourceDummyFramework
                            import CryptoSwift
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

    @Test
    fun `build with custom build path`() {
        val cache = File("/tmp/spm4kmp/cache").also { it.deleteRecursively() }
        val security = File("/tmp/spm4kmp/security").also { it.deleteRecursively() }
        val config = File("/tmp/spm4kmp/config").also { it.deleteRecursively() }
        val customSPMPath = File("/tmp/spm4kmp/workingFile").also { it.deleteRecursively() }
        val localPackageDirectory = File("src/functionalTest/resources/LocalSourceDummyFramework")

        // Given
        val fixture =
            SmpKMPTestFixture
                .builder()
                .withBuildPath(testProjectDir.root.absolutePath)
                .withTargets(AppleCompileTarget.macosX64)
                .withCache(cache.path)
                .withSecurity(security.path)
                .withConfig(config.path)
                .withSPMPath(customSPMPath.path)
                .withSwiftSources(
                    SwiftSource.of(
                        content =
                            """
                            """.trimIndent(),
                    ),
                ).withDependencies(
                    buildList {
                        add(
                            SwiftDependency.Package.Remote.Version(
                                url = URI("https://github.com/krzyzanowskim/CryptoSwift.git"),
                                version = "1.8.3",
                                products = {
                                    add("CryptoSwift")
                                },
                            ),
                        )
                        add(
                            SwiftDependency.Package.Local(
                                path = localPackageDirectory.absolutePath,
                                packageName = "LocalSourceDummyFramework",
                                products = {
                                    add(
                                        ProductName("LocalSourceDummyFramework"),
                                        exportToKotlin = true,
                                    )
                                },
                            ),
                        )
                    },
                ).build()

        // When
        val result =
            GradleBuilder
                .runner(fixture.gradleProject.rootDir, "build")
                .build()

        // Then
        assertThat(result).task(":library:build").succeeded()
        assert(cache.listFiles()?.isNotEmpty() == true)
        assert(config.exists())
        assert(security.exists())
        val scratchDir =
            customSPMPath
                .resolve("spmKmpPlugin")
                .resolve("dummy")
                .resolve("scratch")
        assert(scratchDir.listFiles().isNullOrEmpty() == false)
        cache.deleteRecursively()
        config.deleteRecursively()
        security.deleteRecursively()
        customSPMPath.deleteRecursively()
    }
}
