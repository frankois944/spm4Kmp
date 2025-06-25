package io.github.frankois944.spmForKmp

import com.autonomousapps.kit.GradleBuilder
import com.autonomousapps.kit.truth.TestKitTruth.Companion.assertThat
import io.github.frankois944.spmForKmp.fixture.KotlinSource
import io.github.frankois944.spmForKmp.fixture.SmpKMPTestFixture
import io.github.frankois944.spmForKmp.fixture.SwiftSource
import io.github.frankois944.spmForKmp.utils.BaseTest
import io.github.frankois944.spmForKmp.utils.getExportedPackageContent
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CustomExportedPackageTest : BaseTest() {
    @Test
    fun `build with custom exported package property inclued package`() {
        // Given
        val fixture =
            SmpKMPTestFixture
                .builder()
                .withBuildPath(testProjectDir.root.absolutePath)
                .appendRawPluginRootConfig(
                    """
                    exportedPackageSettings {
                        includeProduct = listOf("KeychainAccess")
                    }
                    """.trimIndent(),
                ).withRawDependencies(
                    KotlinSource.of(
                        content =
                            """
                            remotePackageBranch(
                                url = URI("https://github.com/krzyzanowskim/CryptoSwift.git"),
                                branch = "main",
                                products = {
                                    add(
                                        ProductName("CryptoSwift"),
                                    )
                                },
                            )
                            remotePackageBranch(
                                url = URI("https://github.com/kishikawakatsumi/KeychainAccess.git"),
                                products = {
                                    add("KeychainAccess")
                                },
                                branch = "master",
                            )
                            """.trimIndent(),
                    ),
                ).withKotlinSources(
                    KotlinSource.of(
                        imports = listOf("dummy.MySwiftClass"),
                    ),
                ).withSwiftSources(
                    SwiftSource.of(
                        content =
                            """
                            import Foundation
                            import CryptoSwift
                            import KeychainAccess

                            @objc public class MySwiftClass: NSObject {
                                @objc public func toMD5(value: String) -> String {
                                    return value.md5()
                                }
                            }
                            """.trimIndent(),
                    ),
                ).build()

        val result =
            GradleBuilder
                .runner(fixture.gradleProject.rootDir, "build")
                .build()

        // Then
        assertThat(result).task(":library:build").succeeded()
        val exportedContent = fixture.getExportedPackageContent()
        assertFalse(
            exportedContent.contains("CryptoSwift"),
            "CryptoSwift should not be exported",
        )
        assertTrue(
            exportedContent.contains("KeychainAccess"),
            "KeychainAccess should be exported",
        )
        assertTrue(
            exportedContent.contains("KeychainAccess.git"),
            "KeychainAccess.git should be exported",
        )
    }

    @Test
    fun `build with custom exported package config`() {
        // Given
        val fixture =
            SmpKMPTestFixture
                .builder()
                .withBuildPath(testProjectDir.root.absolutePath)
                .appendRawPluginRootConfig(
                    """
                    exportedPackageSettings {
                        name = "Customexported"
                        isStatic = false
                    }
                    """.trimIndent(),
                ).withRawDependencies(
                    KotlinSource.of(
                        content =
                            """
                            remotePackageBranch(
                               url = URI("https://github.com/krzyzanowskim/CryptoSwift.git"),
                               branch = "main",
                               products = {
                                   add(
                                       ProductName("CryptoSwift"),
                                   )
                               },
                            )
                            """.trimIndent(),
                    ),
                ).withKotlinSources(
                    KotlinSource.of(
                        imports = listOf("dummy.MySwiftClass"),
                    ),
                ).withSwiftSources(
                    SwiftSource.of(
                        content =
                            """
                            import Foundation
                            import CryptoSwift
                            @objc public class MySwiftClass: NSObject {
                                @objc public func toMD5(value: String) -> String {
                                    return value.md5()
                                }
                            }
                            """.trimIndent(),
                    ),
                ).build()

        val result =
            GradleBuilder
                .runner(fixture.gradleProject.rootDir, "build")
                .build()

        // Then
        assertThat(result).task(":library:build").succeeded()
        val exportedContent = fixture.getExportedPackageContent()
        assertTrue(
            exportedContent.contains("name: \"Customexported\""),
            "the package must contains custom name",
        )
        assertTrue(
            exportedContent.contains("type: .dynamic"),
            "the package must contains custom type dynamic",
        )
    }
}
