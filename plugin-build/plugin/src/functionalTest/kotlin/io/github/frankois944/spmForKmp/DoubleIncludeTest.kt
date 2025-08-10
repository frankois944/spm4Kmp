package io.github.frankois944.spmForKmp

import com.autonomousapps.kit.GradleBuilder
import com.autonomousapps.kit.truth.TestKitTruth.Companion.assertThat
import io.github.frankois944.spmForKmp.config.AppleCompileTarget
import io.github.frankois944.spmForKmp.fixture.KotlinSource
import io.github.frankois944.spmForKmp.fixture.SmpKMPTestFixture
import io.github.frankois944.spmForKmp.fixture.SwiftSource
import io.github.frankois944.spmForKmp.utils.BaseTest
import org.junit.jupiter.api.Test

class DoubleIncludeTest : BaseTest() {
    @Test
    fun `build with remote packages with double include`() {
        // Given
        val fixture =
            SmpKMPTestFixture
                .builder()
                .withTargets(AppleCompileTarget.iosSimulatorArm64, AppleCompileTarget.iosArm64)
                .withBuildPath(testProjectDir.root.absolutePath)
                .withMinIos("16.0")
                .withRawDependencies(
                    KotlinSource.of(
                        content =
                            """
                            remotePackageVersion(
                                url = uri("https://github.com/googlemaps/ios-maps-sdk"),
                                version = "10.1.0",
                                products = {
                                    add("GoogleMaps", exportToKotlin = false)
                                },
                            )
                            """.trimIndent(),
                    ),
                ).withKotlinSources(
                    KotlinSource.of(
                        // imports = listOf("GoogleMaps"),
                    ),
                ).withSwiftSources(
                    SwiftSource.of(
                        content =
                            """
                            import GoogleMaps
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
