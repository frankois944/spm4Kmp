package io.github.frankois944.spmForKmp

import com.autonomousapps.kit.GradleBuilder.build
import com.autonomousapps.kit.truth.TestKitTruth.Companion.assertThat
import io.github.frankois944.spmForKmp.definition.SwiftDependency
import io.github.frankois944.spmForKmp.fixture.KotlinSource
import io.github.frankois944.spmForKmp.fixture.SmpKMPTestFixture
import io.github.frankois944.spmForKmp.fixture.SwiftSource
import org.junit.jupiter.api.Test
import java.io.File

class BasicPackageTest {
    @Test
    fun `build with imported UIKit framework is successful`() {
        // Given
        val fixture =
            SmpKMPTestFixture
                .builder()
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
        val result = build(fixture.gradleProject.rootDir, "build")

        // Then
        assertThat(result).task(":library:build").succeeded()
    }

    @Test
    fun `build with custom cache path`() {
        val cache = File("/tmp/spm4kmp/cache").also { it.deleteRecursively() }
        val security = File("/tmp/spm4kmp/security").also { it.deleteRecursively() }
        val config = File("/tmp/spm4kmp/config").also { it.deleteRecursively() }
        val localPackageDirectory = File("src/functionalTest/resources/LocalDummyFramework")

        // Given
        val fixture =
            SmpKMPTestFixture
                .builder()
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
                                packageName = "LocalDummyFramework",
                                exportToKotlin = true,
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
        val result = build(fixture.gradleProject.rootDir, "build")

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
