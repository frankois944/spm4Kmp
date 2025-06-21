package io.github.frankois944.spmForKmp

import com.autonomousapps.kit.GradleBuilder
import com.autonomousapps.kit.truth.TestKitTruth.Companion.assertThat
import io.github.frankois944.spmForKmp.fixture.KotlinSource
import io.github.frankois944.spmForKmp.fixture.SmpKMPTestFixture
import io.github.frankois944.spmForKmp.fixture.SwiftSource
import io.github.frankois944.spmForKmp.utils.BaseTest
import org.junit.jupiter.api.Test

class ErrorPackageTest : BaseTest() {
    @Test
    fun `build with an swift package error binary package`() {
        // Given
        val fixture =
            SmpKMPTestFixture
                .builder()
                .withBuildPath(testProjectDir.root.absolutePath)
                .withMinIos("15.0")
                .withRawDependencies(
                    KotlinSource.of(
                        content =
                            """
remotePackageVersion(
    url = URI("https://github.com/googlemaps/ios-maps-sdk.git"),
    version = "9.3.0",
    products = {
        add(
            ProductName(name = "GoogleMapsTarget", alias = "GoogleMaps"),
            exportToKotlin = true,
        )
    },
)
                            """.trimIndent(),
                    ),
                ).withKotlinSources(
                    KotlinSource.of(
                        imports = listOf("GoogleMapsTarget.*"),
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
