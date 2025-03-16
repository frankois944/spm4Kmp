package io.github.frankois944.spmForKmp

import com.autonomousapps.kit.GradleBuilder
import com.autonomousapps.kit.truth.TestKitTruth.Companion.assertThat
import io.github.frankois944.spmForKmp.config.AppleCompileTarget
import io.github.frankois944.spmForKmp.fixture.SmpKMPTestFixture
import io.github.frankois944.spmForKmp.fixture.SwiftSource
import io.github.frankois944.spmForKmp.utils.BaseTest
import org.junit.jupiter.api.Test

class CustomSettingsTest : BaseTest() {
    @Test
    fun `build with custom target settings`() {
        // Given
        val fixture =
            SmpKMPTestFixture
                .builder()
                .withBuildPath(testProjectDir.root.absolutePath)
                .withMinIos("16.0")
                .withTargets(
                    AppleCompileTarget.iosSimulatorArm64,
                ).appendRawPluginRootConfig(
                    """
                    bridgeSettings {
                        cSetting {
                            defines = listOf(Pair("C_DEBUG", "2"))
                            headerSearchPath = listOf("./includes/")
                            unsafeFlags = listOf("-W")
                        }
                        cxxSetting {
                            defines = listOf(Pair("CXX_DEBUG", "1"))
                            headerSearchPath = listOf("./includes/")
                            unsafeFlags = listOf("-W")
                        }
                        linkerSetting {
                            linkedFramework = listOf("UIKit")
                            linkedLibrary = listOf("-W")
                            unsafeFlags = listOf("-W")
                        }
                        swiftSettings {
                            defines = listOf("CUSTOM_DEFINE")
                            enableExperimentalFeature = "CImplementation"
                            enableUpcomingFeature = "ExistentialAny"
                            interoperabilityMode = "Cxx"
                        }
                    }
                    """,
                ).withSwiftSources(
                    SwiftSource.of(
                        content =
                            """
                            import Foundation
                            #if CUSTOM_DEFINE
                            import UIKit
                            #endif

                            @objc public class MySwiftDummyClass: NSObject {
                                private let view: UIView = UIView()
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
    }
}
