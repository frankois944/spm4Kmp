package io.github.frankois944.spmForKmp

import com.autonomousapps.kit.GradleBuilder
import com.autonomousapps.kit.truth.TestKitTruth.Companion.assertThat
import io.github.frankois944.spmForKmp.fixture.KotlinSource
import io.github.frankois944.spmForKmp.fixture.SmpKMPTestFixture
import io.github.frankois944.spmForKmp.fixture.SwiftSource
import io.github.frankois944.spmForKmp.utils.BaseTest
import org.junit.jupiter.api.Test

class ExoticPackageTest : BaseTest() {
    /**
     * Testing with exporting SDWebImage package.
     * This package has a unique configuration.
     * The objc headers are stored inside the checkout folder instead of the build folder
     */
    @Test
    fun `build with remote packages exotic package with header in checkout`() {
        // Given
        val fixture =
            SmpKMPTestFixture
                .builder()
                .withBuildPath(testProjectDir.root.absolutePath)
                .withGradleCache(false)
                .withRawDependencies(
                    KotlinSource.of(
                        content =
                            """
                            remotePackageVersion(
                                url = URI("https://github.com/SDWebImage/SDWebImage.git"),
                                products = {
                                    add("SDWebImage", exportToKotlin = true)
                                },
                                version = "5.21.3",
                            )
                            """.trimIndent(),
                    ),
                ).withKotlinSources(
                    KotlinSource.of(
                        imports = listOf("SDWebImage.*"),
                    ),
                ).withSwiftSources(
                    SwiftSource.of(
                        content =
                            """
                            import Foundation
                            import SDWebImage
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
