package io.github.frankois944.spmForKmp

import com.autonomousapps.kit.GradleBuilder
import com.autonomousapps.kit.truth.TestKitTruth.Companion.assertThat
import io.github.frankois944.spmForKmp.fixture.KotlinSource
import io.github.frankois944.spmForKmp.fixture.SmpKMPTestFixture
import io.github.frankois944.spmForKmp.fixture.SwiftSource
import io.github.frankois944.spmForKmp.utils.BaseTest
import org.junit.jupiter.api.Test
import java.io.File

/**
 * A cinterop declared by another plugin (or the user) with its own `definitionFile`
 * must not be reconfigured by spmForKmp.
 *
 * Regression test: the plugin used to configure every [org.jetbrains.kotlin.gradle.tasks.CInteropProcess]
 * task of the project, overwriting (nullifying) the `definitionFile` of foreign cinterops.
 * See https://github.com/UbiqueInnovation/uniffi-kotlin-multiplatform-bindings interop issue.
 */
class ForeignCinteropTest : BaseTest() {
    @Test
    fun `foreign cinterop with its own definition file is not modified`() {
        // Given
        val fixture =
            SmpKMPTestFixture
                .builder()
                .withBuildPath(testProjectDir.root.absolutePath)
                .withRawTargetBlock(
                    KotlinSource.of(
                        content =
                            """
                            it.swiftPackageConfig(cinteropName = "dummy") {
                                minIos = "12.0"
                            }
                            it.compilations {
                                val main by getting {
                                    // simulate a cinterop owned by another plugin,
                                    // with a definitionFile at a non-conventional location
                                    cinterops.create("foreignTest") {
                                        definitionFile.set(project.file("foreignTest.def"))
                                    }
                                }
                            }
                            """.trimIndent(),
                    ),
                ).withSwiftSources(
                    SwiftSource.of(
                        content =
                            """
                            import UIKit
                            @objc public class TestView: UIView {}
                            """.trimIndent(),
                    ),
                ).build()

        val projectDir = fixture.gradleProject.rootDir
        File(projectDir, "library/foreignTest.def").writeText(
            """
            language = Objective-C
            package = com.foreign.test
            ---
            """.trimIndent(),
        )

        // When
        val result =
            GradleBuilder
                .runner(projectDir, "build")
                .build()

        // Then
        // Pre-fix, the plugin nullified the foreign definitionFile: the cinterop task then
        // fell back to the conventional path (src/nativeInterop/cinterop/foreignTest.def),
        // which does not exist, and the build failed.
        assertThat(result).task(":library:build").succeeded()
        assertThat(result).task(":library:cinteropForeignTestIosSimulatorArm64").succeeded()
    }
}
