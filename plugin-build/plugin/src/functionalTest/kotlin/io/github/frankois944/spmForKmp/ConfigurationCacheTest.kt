package io.github.frankois944.spmForKmp

import com.autonomousapps.kit.GradleBuilder
import com.autonomousapps.kit.truth.TestKitTruth.Companion.assertThat
import io.github.frankois944.spmForKmp.config.AppleCompileTarget
import io.github.frankois944.spmForKmp.fixture.KotlinSource
import io.github.frankois944.spmForKmp.fixture.SmpKMPTestFixture
import io.github.frankois944.spmForKmp.fixture.SwiftSource
import io.github.frankois944.spmForKmp.utils.BaseTest
import org.gradle.testkit.runner.BuildResult
import org.junit.jupiter.api.Test
import java.io.File

/**
 * The plugin must work with the configuration cache: everything a task needs is set from the DSL
 * while the project is configured, so the task graph can be serialized and replayed.
 *
 * Only the configuration side is covered here. Executing a task of the plugin under TestKit with
 * the configuration cache on fails in `withPluginClasspath()`, unrelated to the plugin: Gradle
 * cannot load the classes of the injected plugin classpath back when it snapshots the
 * dependencies declared as task inputs (`NoClassDefFoundError` on a DSL class). The `:example`
 * module builds with `org.gradle.configuration-cache=true` and covers the execution side.
 */
class ConfigurationCacheTest : BaseTest() {
    @Test
    fun `the configuration cache is stored then reused`() {
        val fixture = fixtureWithEveryDependencyType()
        val projectDir = fixture.gradleProject.rootDir

        // A configuration cache problem fails the build, so succeeding is already meaningful:
        // it means every task of the plugin could be stored in the entry.
        val firstBuild = GradleBuilder.runner(projectDir, TASKS_TASK, CONFIGURATION_CACHE).build()
        assertThat(firstBuild).task(TASKS_TASK).succeeded()
        assert(firstBuild.configurationCacheDecision() == "stored") {
            "the first run did not store a configuration cache entry"
        }

        // The plugin configured its task graph, and not a subset of it.
        listOf(RESOLVE_TASK, COMPILE_TASK, MANIFEST_TASK).forEach { task ->
            assert(firstBuild.output.contains(task)) {
                "$task is missing from the task listing of the configured project"
            }
        }

        val secondBuild = GradleBuilder.runner(projectDir, TASKS_TASK, CONFIGURATION_CACHE).build()
        assertThat(secondBuild).task(TASKS_TASK).succeeded()
        assert(secondBuild.configurationCacheDecision() == "reused") {
            "the second run did not reuse the configuration cache entry, it configured again"
        }
    }

    @Test
    fun `changing the package configuration invalidates the configuration cache`() {
        val fixture = fixtureWithEveryDependencyType()
        val projectDir = fixture.gradleProject.rootDir

        GradleBuilder.runner(projectDir, TASKS_TASK, CONFIGURATION_CACHE).build()

        // The dependencies are declared in the build script: editing it must configure again,
        // otherwise a stale task graph would be replayed.
        val buildScript = File(projectDir, "library/build.gradle.kts")
        buildScript.appendText("\n// touched\n")

        val afterEdit = GradleBuilder.runner(projectDir, TASKS_TASK, CONFIGURATION_CACHE).build()
        assertThat(afterEdit).task(TASKS_TASK).succeeded()
        assert(afterEdit.configurationCacheDecision() == "stored") {
            "editing the build script did not invalidate the configuration cache entry"
        }
    }

    private fun fixtureWithEveryDependencyType(): SmpKMPTestFixture {
        val localPackageDirectory = File("src/functionalTest/resources/LocalSourceDummyFramework")
        return SmpKMPTestFixture
            .builder()
            .withBuildPath(testProjectDir.root.absolutePath)
            .withTargets(AppleCompileTarget.iosSimulatorArm64)
            .withRawDependencies(
                KotlinSource.of(
                    content =
                        """
                        remoteBinary(
                            url = URI("https://spmforkmp.eu/DummyFrameworkV2.xcframework.zip"),
                            checksum = "90da1dfbf1b52b647958974002a329a60e291b463fcb69a53e2e42b74ead0a94",
                            packageName = "DummyFramework",
                            exportToKotlin = true
                        )
                        localPackage(
                            path = "${localPackageDirectory.absolutePath}",
                            packageName = "LocalSourceDummyFramework",
                            products = {
                                add("LocalSourceDummyFramework", exportToKotlin = true)
                            },
                        )
                        remotePackageVersion(
                            url = URI("https://github.com/krzyzanowskim/CryptoSwift.git"),
                            products = {
                                add("CryptoSwift")
                            },
                            version = "1.8.3",
                        )
                        """.trimIndent(),
                ),
            ).withSwiftSources(
                SwiftSource.of(
                    content =
                        """
                        import Foundation
                        @objc public class MySwiftClass: NSObject {
                        }
                        """.trimIndent(),
                ),
            ).build()
    }

    /**
     * What Gradle did with the configuration cache, as its last decision of the run:
     * `stored`, `reused` or `discarded`.
     */
    private fun BuildResult.configurationCacheDecision(): String? =
        ENTRY_DECISION
            .findAll(output)
            .lastOrNull()
            ?.groupValues
            ?.get(1)

    private companion object {
        const val CONFIGURATION_CACHE = "--configuration-cache"
        const val TASKS_TASK = ":library:tasks"
        const val MANIFEST_TASK = "SwiftPackageConfigAppleDummyGenerateSwiftPackage"
        const val RESOLVE_TASK = "SwiftPackageConfigAppleDummyResolveSwiftPackage"
        const val COMPILE_TASK = "SwiftPackageConfigAppleDummyCompileSwiftPackageIosSimulatorArm64"

        val ENTRY_DECISION = """Configuration cache entry (\w+)""".toRegex()
    }
}
