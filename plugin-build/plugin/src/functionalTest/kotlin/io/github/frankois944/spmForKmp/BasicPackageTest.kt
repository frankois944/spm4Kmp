package io.github.frankois944.spmForKmp

import com.autonomousapps.kit.GradleBuilder
import com.autonomousapps.kit.truth.TestKitTruth.Companion.assertThat
import io.github.frankois944.spmForKmp.definition.ProductPackageConfig
import io.github.frankois944.spmForKmp.definition.SwiftDependency
import io.github.frankois944.spmForKmp.fixture.KotlinSource
import io.github.frankois944.spmForKmp.fixture.SmpKMPTestFixture
import io.github.frankois944.spmForKmp.fixture.SwiftSource
import io.github.frankois944.spmForKmp.utils.BaseTest
import org.junit.jupiter.api.Test
import java.io.File

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
                        content =
                            """
                            package com.example
                            import dummy.TestView
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
    fun `build with custom cache path`() {
        val cache = File("/tmp/spm4kmp/cache").also { it.deleteRecursively() }
        val security = File("/tmp/spm4kmp/security").also { it.deleteRecursively() }
        val config = File("/tmp/spm4kmp/config").also { it.deleteRecursively() }
        val localPackageDirectory = File("src/functionalTest/resources/LocalSourceDummyFramework")

        // Given
        val fixture =
            SmpKMPTestFixture
                .builder()
                .withBuildPath(testProjectDir.root.absolutePath)
                .withTargets(CompileTarget.macosX64)
                .withCache(cache.path)
                .withSecurity(security.path)
                .withConfig(config.path)
                .withSwiftSources(
                    SwiftSource.of(
                        content =
                            """
                            """.trimIndent(),
                    ),
                ).withDependencies(
                    buildList {
                        add(
                            SwiftDependency.Package.Local(
                                path = localPackageDirectory.absolutePath,
                                packageName = "LocalSourceDummyFramework",
                                products =
                                    listOf(
                                        ProductPackageConfig(
                                            "LocalSourceDummyFramework",
                                            exportToKotlin = true,
                                        ),
                                    ),
                            ),
                        )
                    },
                ).withKotlinSources(
                    KotlinSource.of(
                        content =
                            """
                            package com.example
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
        assert(cache.listFiles()?.isNotEmpty() == true)
        assert(config.exists())
        assert(security.exists())
        cache.deleteRecursively()
        config.deleteRecursively()
        security.deleteRecursively()
    }
}
