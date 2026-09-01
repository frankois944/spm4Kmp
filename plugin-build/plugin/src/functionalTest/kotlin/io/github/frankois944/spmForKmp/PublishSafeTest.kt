package io.github.frankois944.spmForKmp

import com.autonomousapps.kit.GradleBuilder
import com.autonomousapps.kit.truth.TestKitTruth.Companion.assertThat
import io.github.frankois944.spmForKmp.config.AppleCompileTarget
import io.github.frankois944.spmForKmp.fixture.KotlinSource
import io.github.frankois944.spmForKmp.fixture.SmpKMPTestFixture
import io.github.frankois944.spmForKmp.fixture.SwiftSource
import io.github.frankois944.spmForKmp.utils.BaseTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * `cinterop` copies `libraryPaths` and `linkerOpts` from the definition file straight into the
 * klib manifest. When they hold the absolute scratch directory of the publishing machine, every
 * consumer of the published library gets `ld: warning: search path ... not found`.
 *
 * With `publishSafe = true` the definitions must stay free of any path of the current machine,
 * while the project itself must still build and link (the search paths are added back on the
 * link tasks of the project owning the Swift package).
 */
class PublishSafeTest : BaseTest() {
    @Test
    fun `publishSafe definitions contain no machine specific link path`() {
        // Given
        val localPackageDirectory = File("src/functionalTest/resources/LocalSourceDummyFramework")
        val fixture =
            SmpKMPTestFixture
                .builder()
                .withBuildPath(testProjectDir.root.absolutePath)
                .withTargets(AppleCompileTarget.iosSimulatorArm64)
                .appendRawPluginRootConfig(
                    """
                    publishSafe = true
                    """,
                ).withRawDependencies(
                    KotlinSource.of(
                        content =
                            """
                            localPackage(
                                path = "${localPackageDirectory.absolutePath}",
                                packageName = "LocalSourceDummyFramework",
                                products = {
                                    add("LocalSourceDummyFramework", exportToKotlin = true)
                                },
                            )
                            """.trimIndent(),
                    ),
                ).withSwiftSources(
                    SwiftSource.of(
                        content =
                            """
                            import Foundation
                            import LocalSourceDummyFramework

                            @objc public class PublishSafeBridge: NSObject {
                                @objc public func ping() -> String {
                                    return "pong"
                                }
                            }
                            """.trimIndent(),
                    ),
                ).withKotlinSources(
                    KotlinSource.of(
                        imports = listOf("dummy.PublishSafeBridge"),
                    ),
                ).build()

        // When
        val result =
            GradleBuilder
                .runner(fixture.gradleProject.rootDir, "build")
                .build()

        // Then
        assertThat(result).task(":library:build").succeeded()

        val definitions =
            fixture.gradleProject.rootDir
                .resolve("library/build/spmKmpPlugin/dummy/defFiles")
                .walkTopDown()
                .filter { it.isFile && it.extension == "def" }
                .toList()

        assertTrue(definitions.isNotEmpty(), "No definition file was generated")

        definitions.forEach { definition ->
            val content = definition.readText()
            // The bridge definition is the one carrying `staticLibraries`; cinterop needs
            // `libraryPaths` to resolve that archive and embed it into the klib, so it stays.
            // Every other definition resolves nothing through it and must not declare it.
            if (!definition.name.endsWith("_bridge.def")) {
                assertTrue(
                    !content.contains("libraryPaths"),
                    "${definition.name} must not declare libraryPaths in publishSafe mode\n$content",
                )
            }
            content
                .lines()
                .filter { it.startsWith("linkerOpts") }
                .forEach { line ->
                    assertTrue(
                        !line.contains("-F\"/") && !line.contains("-L\"/"),
                        "${definition.name} must not carry an absolute search path\n$line",
                    )
                    assertTrue(
                        !line.contains(".xctoolchain"),
                        "${definition.name} must not carry the local Xcode toolchain path\n$line",
                    )
                }
        }
    }
}
