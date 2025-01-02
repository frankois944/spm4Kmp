package io.github.frankois944.spmForKmp

import com.autonomousapps.kit.GradleBuilder.build
import com.autonomousapps.kit.truth.TestKitTruth.Companion.assertThat
import io.github.frankois944.spmForKmp.fixture.KotlinSource
import io.github.frankois944.spmForKmp.fixture.SmpKMPTestFixture
import io.github.frankois944.spmForKmp.fixture.SwiftSource
import org.junit.jupiter.api.Test

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
}