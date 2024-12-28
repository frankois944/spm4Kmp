package fr.frankois944.spm.kmp.plugin

import com.autonomousapps.kit.GradleBuilder.build
import com.autonomousapps.kit.truth.TestKitTruth.Companion.assertThat
import fr.frankois944.spm.kmp.plugin.definition.SwiftPackageDependencyDefinition
import fr.frankois944.spm.kmp.plugin.fixture.SmpKMPTestFixture
import fr.frankois944.spm.kmp.plugin.fixture.SwiftSource
import org.junit.jupiter.api.Test

class BasicModuleTest {
    @Test
    fun `build simple remote package`() {
        // Given
        val fixture =
            SmpKMPTestFixture
                .builder()
                .withDependencies(
                    buildList {
                        add(
                            SwiftPackageDependencyDefinition.RemoteDefinition.Version(
                                url = "https://github.com/krzyzanowskim/CryptoSwift.git",
                                names = listOf("CryptoSwift"),
                                version = "1.8.3",
                            ),
                        )
                    },
                ).withSwiftSources(
                    SwiftSource.of(
                        content =
                            """
                            import Foundation
                            import CryptoSwift
                            @objc public class CryptoSwift: NSObject {
                                @objc public func toMD5(value: String) -> String {
                                    return value.md5()
                                }
                            }
                            """.trimIndent(),
                    ),
                ).build()

        // When
        val result = build(fixture.gradleProject.rootDir, "build")

        // Then
        assertThat(result).task(":library:build").succeeded()
        assertPackageResolved(fixture, "CryptoSwift")
    }
}
