package io.github.frankois944.spmForKmp

import com.autonomousapps.kit.GradleBuilder
import com.autonomousapps.kit.truth.TestKitTruth.Companion.assertThat
import io.github.frankois944.spmForKmp.fixture.KotlinSource
import io.github.frankois944.spmForKmp.fixture.SmpKMPTestFixture
import io.github.frankois944.spmForKmp.fixture.SwiftSource
import io.github.frankois944.spmForKmp.utils.BaseTest
import org.junit.jupiter.api.Test
import java.io.File

class NewApiTest : BaseTest() {
    @Test
    fun `build with new configuration`() {
        val localPackageDirectory = File("src/functionalTest/resources/LocalSourceDummyFramework").absolutePath

        val fixture =
            SmpKMPTestFixture
                .builder()
                .withBuildPath(testProjectDir.root.absolutePath)
                .withRawPluginConfiguration(
                    KotlinSource.of(
                        content =
                            """
dependency {
    remoteBinary(
        url =
            URI(
                "https://raw.githubusercontent.com/" +
                    "frankois944/spm4Kmp/refs/heads/main/plugin-build/" +
                    "plugin/src/functionalTest/resources/DummyFrameworkV2.xcframework.zip",
            ),
        checksum = "ce79ee72991d40620f156e407e47145ba3279cc15ebf20bb2560c7882d69e56e",
        packageName = "DummyFramework",
        exportToKotlin = true,
    )
    localPackage(
        path = "$localPackageDirectory",
        packageName = "LocalSourceDummyFramework",
        products = {
            // Export to Kotlin for use in shared Kotlin code, false by default
            add("LocalSourceDummyFramework", exportToKotlin = true)
        },
    )
    remotePackageVersion(
        url = URI("https://github.com/krzyzanowskim/CryptoSwift.git"),
        version = "1.8.1",
        products = {

            add("CryptoSwift")
        },
    )
    remotePackageBranch(
        url = URI("https://github.com/FluidGroup/JAYSON"),
        products = {
            add("JAYSON")
        },
        branch = "main",
    )
    remotePackageCommit(
        url = URI("https://github.com/venmo/Static"),
        products = {
            add("Static")
        },
        revision = "622a6804d39515600ead16e6259cb5d5e50f40df",
    )
}
                            """.trimIndent(),
                    ),
                ).withSwiftSources(
                    SwiftSource.of(
                        content =
                            """
                            import Foundation
                            import LocalSourceDummyFramework
                            @objc public class MySwiftDummyClass: NSObject {
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

    @Test
    fun `build with simple new configuration`() {
        val localDirectory = File("src/functionalTest/resources/DummyFramework.xcframework").absolutePath

        val fixture =
            SmpKMPTestFixture
                .builder()
                .withBuildPath(testProjectDir.root.absolutePath)
                .withRawPluginConfiguration(
                    KotlinSource.of(
                        content =
                            """
dependency {
    localBinary(
        path = "$localDirectory",
        packageName = "DummyFramework",
    )
}
                            """.trimIndent(),
                    ),
                ).withSwiftSources(
                    SwiftSource.of(
                        content =
                            """
                            import Foundation
                            @objc public class MySwiftDummyClass: NSObject {
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
