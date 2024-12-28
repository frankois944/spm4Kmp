package fr.frankois944.spm.kmp.plugin

import com.autonomousapps.kit.GradleBuilder.build
import com.autonomousapps.kit.truth.TestKitTruth.Companion.assertThat
import fr.frankois944.spm.kmp.plugin.fixture.SmpKMPTestFixture
import fr.frankois944.spm.kmp.plugin.fixture.SwiftSource
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class SwiftPackageModulesTest {
    @Test
    fun `build with remote SPM dependency using exact version is successful`() {
        // Given
        val fixture =
            SmpKMPTestFixture
                .builder()
                .withSwiftSources(
                    SwiftSource.of(
                        content =
                            """
                            import Foundation
                            import KeychainAccess

                            @objc public class KeychainManager: NSObject {
                                private let keychain = Keychain(service: "test-service")

                                @objc public func save(value: String, forKey key: String) throws {
                                    try keychain.set(value, key: key)
                                }
                            }
                            """.trimIndent(),
                    ),
                ).build()

        // When
        val result = build(fixture.gradleProject.rootDir, "build")

        // Then
        assertThat(result).task(":library:build").succeeded()
        assertPackageResolved(fixture, "KeychainAccess")
    }

    private fun assertPackageResolved(
        fixture: SmpKMPTestFixture,
        vararg packageNames: String,
    ) {
        val resolvedFile =
            File(
                fixture.gradleProject.rootDir,
                "library/build/swiftklib/test/iosArm64/swiftBuild/Package.resolved",
            )
        assertTrue(resolvedFile.exists(), "Package.resolved file not found")

        getPackageResolvedContent(fixture) { content ->
            packageNames.forEach { packageName ->
                assertTrue(
                    content.contains("\"identity\" : \"$packageName\"", ignoreCase = true),
                    "$packageName dependency not found",
                )
            }
        }
    }

    private fun getManifestContent(
        fixture: SmpKMPTestFixture,
        content: (String) -> Unit,
    ) {
        val resolvedFile =
            File(
                fixture.gradleProject.rootDir,
                "library/build/swiftklib/test/iosArm64/swiftBuild/Package.swift",
            )
        assertTrue(resolvedFile.exists(), "Package.swift file not found")
        content(resolvedFile.readText())
    }

    private fun getPackageResolvedContent(
        fixture: SmpKMPTestFixture,
        content: (String) -> Unit,
    ) {
        val resolvedFile =
            File(
                fixture.gradleProject.rootDir,
                "library/build/swiftklib/test/iosArm64/swiftBuild/Package.resolved",
            )
        assertTrue(resolvedFile.exists(), "Package.resolved file not found")
        content(resolvedFile.readText())
    }
}
