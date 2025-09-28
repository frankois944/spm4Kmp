package io.github.frankois944.spmForKmp

import com.autonomousapps.kit.GradleBuilder
import com.autonomousapps.kit.truth.TestKitTruth.Companion.assertThat
import io.github.frankois944.spmForKmp.fixture.KotlinSource
import io.github.frankois944.spmForKmp.fixture.SmpKMPTestFixture
import io.github.frankois944.spmForKmp.fixture.SwiftSource
import io.github.frankois944.spmForKmp.utils.BaseTest
import org.junit.jupiter.api.Test

class CLanguageBinaryPackageTest : BaseTest() {
    @Test
    fun `build with remote binary with C language package`() {
        // Given
        val fixture =
            SmpKMPTestFixture
                .builder()
                .withBuildPath(testProjectDir.root.absolutePath)
                .withRawDependencies(
                    KotlinSource.of(
                        content =
                            """
                            remoteBinary(
                                url = uri("https://github.com/wanliyunyan/HevSocks5Tunnel/releases/download/2.10.0/HevSocks5Tunnel.xcframework.zip"),
                                packageName = "HevSocks5Tunnel",
                                exportToKotlin = true,
                                checksum = "f66fc314edbdb7611c5e8522bc50ee62e7930f37f80631b8d08b2a40c81a631a",
                                isCLang = true,
                            )
                            """.trimIndent(),
                    ),
                ).withKotlinSources(
                    KotlinSource.of(
                        imports = listOf("HevSocks5Tunnel.hev_socks5_tunnel_quit"),
                        content =
                            """
                            @OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
                            fun test() {
                                hev_socks5_tunnel_quit()
                            }
                            """.trimIndent(),
                    ),
                ).withSwiftSources(
                    SwiftSource.of(
                        content =
                            """
                            import Foundation
                            import HevSocks5Tunnel
                            @objc public class MySwiftClass: NSObject {
                                public func cMethod() {
                                    hev_socks5_tunnel_quit()
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
    }
}
