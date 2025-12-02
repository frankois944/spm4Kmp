package io.github.frankois944.spmForKmp

import com.autonomousapps.kit.GradleBuilder
import com.autonomousapps.kit.truth.TestKitTruth.Companion.assertThat
import io.github.frankois944.spmForKmp.fixture.KotlinSource
import io.github.frankois944.spmForKmp.fixture.SmpKMPTestFixture
import io.github.frankois944.spmForKmp.fixture.SwiftSource
import io.github.frankois944.spmForKmp.utils.BaseTest
import org.junit.jupiter.api.Test

class PackageRegistryTest : BaseTest() {
    @Test
    fun `build with package registry`() {
        val token = System.getenv("GITEA_TOKEN")
        if (token == null) {
            println("SKIP TEST because no GITEA_TOKEN set")
            return
        }
        val fixture =
            SmpKMPTestFixture
                .builder()
                .withBuildPath(testProjectDir.root.absolutePath)
                .withRawTargetBlock(
                    KotlinSource.of(
                        content =
                            """
                            it.swiftPackageConfig(cinteropName = "nativeIosShared") {
                                registry(
                                    url = uri("https://spm.azodus.blog/api/packages/frankois944/swift"),
                                    token = "$token",
                                )
                                minIos = "16.0"
                                dependency {
                                    registryPackage(
                                        id = "spm.dummy",
                                        version = "1.0.1",
                                        products = {
                                            add("registrydummy", exportToKotlin = true)
                                        },
                                    )
                                }
                            }
                            """.trimIndent(),
                    ),
                ).withKotlinSources(
                    KotlinSource.of(
                        imports = listOf("registrydummy.RegistryDummy"),
                    ),
                ).withSwiftSources(
                    SwiftSource.of(
                        content =
                            """
                            import registrydummy
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
