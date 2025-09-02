package io.github.frankois944.spmForKmp

import com.autonomousapps.kit.GradleBuilder
import com.autonomousapps.kit.truth.TestKitTruth.Companion.assertThat
import io.github.frankois944.spmForKmp.fixture.KotlinSource
import io.github.frankois944.spmForKmp.fixture.SmpKMPTestFixture
import io.github.frankois944.spmForKmp.fixture.SwiftSource
import io.github.frankois944.spmForKmp.utils.BaseTest
import org.junit.jupiter.api.Test

class CustomDefinitionTests : BaseTest() {
    @Test
    fun `build with custom definition config`() {
        // Given
        val fixture =
            SmpKMPTestFixture
                .builder()
                .withBuildPath(testProjectDir.root.absolutePath)
                .appendRawPluginRootConfig(
                    """
                    strictEnums = listOf("MyEnum1", "MyEnum1")
                    nonStrictEnums = listOf("MyEnum2", "MyEnum2")
                    foreignExceptionMode = "objc-wrap"
                    disableDesignatedInitializerChecks = false
                    userSetupHint = "Some Hint"
                    """.trimIndent(),
                ).withSwiftSources(
                    SwiftSource.of(
                        content =
                            """
                            import Foundation
                            @objc public enum MyEnum1: Int {
                                case EnumVal1 = 1
                            }
                            @objc public enum MyEnum2: Int {
                                case EnumVal2 = 2
                            }

                             @objc public class TestView: NSObject {
                                @objc public var myEnumVal1: MyEnum1 = .EnumVal1
                                @objc public var myEnumVal2: MyEnum2 = .EnumVal2
                             }
                            """.trimIndent(),
                    ),
                ).withKotlinSources(
                    KotlinSource.of(
                        imports = listOf("dummy.MyEnum1", "dummy.MyEnum2EnumVal2", "kotlinx.cinterop.ExperimentalForeignApi"),
                        content =
                            """
@OptIn(ExperimentalForeignApi::class)
val test1 = MyEnum1.MyEnum1EnumVal1
@OptIn(ExperimentalForeignApi::class)
val test2 = MyEnum2EnumVal2
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
