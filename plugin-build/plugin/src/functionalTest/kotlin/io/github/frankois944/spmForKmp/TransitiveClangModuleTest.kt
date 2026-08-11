package io.github.frankois944.spmForKmp

import com.autonomousapps.kit.GradleBuilder
import com.autonomousapps.kit.truth.TestKitTruth.Companion.assertThat
import io.github.frankois944.spmForKmp.fixture.KotlinSource
import io.github.frankois944.spmForKmp.fixture.SmpKMPTestFixture
import io.github.frankois944.spmForKmp.fixture.SwiftSource
import io.github.frankois944.spmForKmp.utils.BaseTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * The bridge definition file must include the build directories of transitive
 * Clang/ObjC modules (ie. `<Module>.build`) so that cinterop can resolve
 * `@import <Module>;` statements emitted in the generated Swift header.
 *
 * Regression test: the `.build` include paths were only added to the definition
 * of exported dependency modules, not to the bridge module itself; Kotlin 2.4.0
 * cinterop then failed with `module 'NearbyCoreAdapter' not found`.
 * See the dependency chain: bridge -> NearbyConnections -> NearbyCoreAdapter (Clang).
 */
class TransitiveClangModuleTest : BaseTest() {
    @Test
    fun `bridge definition contains transitive clang module build directories`() {
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
                            remotePackageCommit(
                                url = URI("https://github.com/google/nearby.git"),
                                revision = "43213ff70710b9993f3f351582b660f938c40bd6",
                                products = {
                                    add("NearbyConnections")
                                },
                            )
                            """.trimIndent(),
                    ),
                ).withKotlinSources(
                    KotlinSource.of(
                        imports = listOf("dummy.NearbyTestBridge"),
                    ),
                ).withSwiftSources(
                    SwiftSource.of(
                        content =
                            """
                            import Foundation
                            import NearbyConnections

                            @objc public class NearbyTestBridge: NSObject {
                                @objc public func ping() -> String {
                                    return "pong"
                                }
                            }
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

        val bridgeDefinition =
            checkNotNull(
                fixture.gradleProject.rootDir
                    .resolve("library/build/spmKmpPlugin/dummy")
                    .walkTopDown()
                    .firstOrNull { it.name == "dummy_bridge.def" },
            ) { "The bridge definition file must exist" }
        val definitionContent = bridgeDefinition.readText()
        assertTrue(
            definitionContent.contains("NearbyCoreAdapter.build"),
            "The bridge definition must include the transitive Clang module build directory\n$definitionContent",
        )
    }
}
