package io.github.frankois944.spmForKmp

import com.autonomousapps.kit.GradleBuilder
import com.autonomousapps.kit.truth.TestKitTruth.Companion.assertThat
import io.github.frankois944.spmForKmp.config.AppleCompileTarget
import io.github.frankois944.spmForKmp.fixture.KotlinSource
import io.github.frankois944.spmForKmp.fixture.SmpKMPTestFixture
import io.github.frankois944.spmForKmp.fixture.SwiftSource
import io.github.frankois944.spmForKmp.utils.BaseTest
import org.junit.jupiter.api.Test

class TvOSTest : BaseTest() {
    @Test
    fun `build for watchOS`() {
        val fixture =
            SmpKMPTestFixture
                .builder()
                .withBuildPath(testProjectDir.root.absolutePath)
                .withTargets(
                    AppleCompileTarget.tvosSimulatorArm64,
                    AppleCompileTarget.tvosArm64,
                    AppleCompileTarget.tvosX64,
                )
                .withRawTargetBlock(
                    KotlinSource.of(
                        content =
                            """
                            it.swiftPackageConfig(cinteropName = "dummy") {
                            }
                            """.trimIndent(),
                    ),
                )
                .withKotlinSources(
                    KotlinSource.of(
                        imports = listOf("dummy.*"),
                        content =
                            """
                            @kotlinx.cinterop.ExperimentalForeignApi
                            val view = ObjectCls()
                            """.trimIndent(),
                    ),
                )
                .withSwiftSources(
                    SwiftSource.of(
                        content =
                            """
                            import TVUIKit
                            func dummyFunction() {
                                // Poster view — expands when focused (parallax)
                                let poster = TVPosterView(image: UIImage(named: "movie"))
                                poster.title = "Inception"
                                poster.subtitle = "Christopher Nolan · 2010"
                            }

                             @objc public class ObjectCls: NSObject {}
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
