package io.github.frankois944.spmForKmp

import com.autonomousapps.kit.GradleBuilder
import com.autonomousapps.kit.truth.TestKitTruth.Companion.assertThat
import io.github.frankois944.spmForKmp.config.AppleCompileTarget
import io.github.frankois944.spmForKmp.fixture.KotlinSource
import io.github.frankois944.spmForKmp.fixture.SmpKMPTestFixture
import io.github.frankois944.spmForKmp.fixture.SwiftSource
import io.github.frankois944.spmForKmp.utils.BaseTest
import io.github.frankois944.spmForKmp.utils.TestBuildSystem
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Regression tests for https://github.com/frankois944/spm4Kmp/issues/329
 *
 * The products of `swift build` reference, by absolute path, files living outside of the build
 * directory: the headers of the binary dependencies in `scratch/artifacts` and the umbrella
 * headers of the C/Objective-C dependencies in `scratch/checkouts`. A build cache entry only
 * carries the build directory, so those two directories are resolved by their own task, which
 * runs whenever they are missing.
 *
 * Every test here runs the same scenario: a cold build that populates the cache, a `clean` that
 * wipes the build directory (like a fresh clone on CI with a warm cache), then a warm build whose
 * compile task is served from the cache and must still produce a working cinterop.
 */
class BuildCacheTest : BaseTest() {
    @Test
    fun `remote binary package survives a build cache hit`() {
        val fixture =
            cacheFixtureBuilder()
                .withTargets(AppleCompileTarget.iosSimulatorArm64)
                .withRawDependencies(KotlinSource.of(content = REMOTE_BINARY_DEPENDENCY))
                .withKotlinSources(
                    KotlinSource.of(imports = listOf("DummyFramework.DummyFrameworkVersionNumber")),
                ).withSwiftSources(
                    SwiftSource.of(content = swiftSource("DummyFramework")),
                ).build()

        assertSurvivesBuildCacheHit(
            fixture = fixture,
            targets = listOf(AppleCompileTarget.iosSimulatorArm64),
        )
    }

    @Test
    fun `remote binary package survives a build cache hit with several targets`() {
        val targets = listOf(AppleCompileTarget.iosSimulatorArm64, AppleCompileTarget.iosArm64)
        val fixture =
            cacheFixtureBuilder()
                .withTargets(*targets.toTypedArray())
                .withRawDependencies(KotlinSource.of(content = REMOTE_BINARY_DEPENDENCY))
                .withKotlinSources(
                    KotlinSource.of(imports = listOf("DummyFramework.DummyFrameworkVersionNumber")),
                ).withSwiftSources(
                    SwiftSource.of(content = swiftSource("DummyFramework")),
                ).build()

        assertSurvivesBuildCacheHit(fixture = fixture, targets = targets)
    }

    @Test
    fun `every dependency type survives a build cache hit`() {
        val localPackageDirectory = File("src/functionalTest/resources/LocalSourceDummyFramework")
        val targets = listOf(AppleCompileTarget.iosSimulatorArm64, AppleCompileTarget.iosArm64)
        val fixture =
            cacheFixtureBuilder()
                .withTargets(*targets.toTypedArray())
                .withRawDependencies(
                    KotlinSource.of(
                        content =
                            """
                            $REMOTE_BINARY_DEPENDENCY
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
                ).withKotlinSources(
                    KotlinSource.of(
                        imports =
                            listOf(
                                "DummyFramework.DummyFrameworkVersionNumber",
                                "LocalSourceDummyFramework.LocalSourceDummy",
                            ),
                    ),
                ).withSwiftSources(
                    SwiftSource.of(
                        content =
                            swiftSource(
                                "DummyFramework",
                                "LocalSourceDummyFramework",
                                "CryptoSwift",
                            ),
                    ),
                ).build()

        assertSurvivesBuildCacheHit(
            fixture = fixture,
            targets = targets,
            scratchDirs = listOf("artifacts", "checkouts"),
        )
    }

    @Test
    fun `objective-c package with resources survives a build cache hit`() {
        // SDWebImage covers the two things a cache entry cannot carry on its own: the module map
        // SwiftPM generates points at the umbrella header in `scratch/checkouts`, and its resource
        // bundle contains a relative symbolic link Gradle is unable to pack.
        val fixture =
            cacheFixtureBuilder()
                .withTargets(AppleCompileTarget.iosSimulatorArm64)
                .withRawDependencies(
                    KotlinSource.of(
                        content =
                            """
                            remotePackageVersion(
                                url = URI("https://github.com/SDWebImage/SDWebImage.git"),
                                products = {
                                    add("SDWebImage", exportToKotlin = true)
                                },
                                version = "5.21.3",
                            )
                            """.trimIndent(),
                    ),
                ).withKotlinSources(
                    KotlinSource.of(imports = listOf("SDWebImage.*")),
                ).withSwiftSources(
                    SwiftSource.of(content = swiftSource("SDWebImage")),
                ).build()

        assertSurvivesBuildCacheHit(
            fixture = fixture,
            targets = listOf(AppleCompileTarget.iosSimulatorArm64),
            scratchDirs = listOf("checkouts"),
        )
    }

    @Test
    fun `an unchanged build recompiles nothing`() {
        val fixture =
            cacheFixtureBuilder()
                .withTargets(AppleCompileTarget.iosSimulatorArm64)
                .withRawDependencies(KotlinSource.of(content = REMOTE_BINARY_DEPENDENCY))
                .withKotlinSources(
                    KotlinSource.of(imports = listOf("DummyFramework.DummyFrameworkVersionNumber")),
                ).withSwiftSources(
                    SwiftSource.of(content = swiftSource("DummyFramework")),
                ).build()
        val projectDir = fixture.gradleProject.rootDir

        GradleBuilder.runner(projectDir, "build", "--build-cache").build()
        val secondBuild = GradleBuilder.runner(projectDir, "build", "--build-cache").build()

        // The resolve task checks the presence of the checkouts to decide whether it is up to
        // date: that check must not make it, and the compile task behind it, run on every build.
        assert(secondBuild.task(":library:$RESOLVE_TASK_NAME")?.outcome == TaskOutcome.UP_TO_DATE) {
            "the resolve task ran again on an unchanged build"
        }
        val compileOutcome =
            secondBuild.task(":library:${compileTaskName(AppleCompileTarget.iosSimulatorArm64)}")?.outcome
        assert(compileOutcome == TaskOutcome.UP_TO_DATE) {
            "the compile task ran again on an unchanged build, it was $compileOutcome"
        }
    }

    @Test
    fun `deleting the resolved dependencies makes them come back`() {
        // What the resolve task exists for: the build directory can be restored from the cache
        // while the dependencies it points at are gone.
        val fixture =
            cacheFixtureBuilder()
                .withTargets(AppleCompileTarget.iosSimulatorArm64)
                .withRawDependencies(
                    KotlinSource.of(
                        content =
                            """
                            $REMOTE_BINARY_DEPENDENCY
                            remotePackageVersion(
                                url = URI("https://github.com/krzyzanowskim/CryptoSwift.git"),
                                products = {
                                    add("CryptoSwift")
                                },
                                version = "1.8.3",
                            )
                            """.trimIndent(),
                    ),
                ).withKotlinSources(
                    KotlinSource.of(imports = listOf("DummyFramework.DummyFrameworkVersionNumber")),
                ).withSwiftSources(
                    SwiftSource.of(content = swiftSource("DummyFramework", "CryptoSwift")),
                ).build()
        val projectDir = fixture.gradleProject.rootDir

        GradleBuilder.runner(projectDir, "build", "--build-cache").build()

        listOf("artifacts", "checkouts").forEach { name ->
            val dir = File(projectDir, "$RESOLVE_PATH/$name")
            assert(dir.deleteRecursively()) { "could not delete $dir" }

            // `build()` fails the test on a broken build, so reaching the assertions below
            // already means the cinterop step survived the loss of the directory.
            val build = GradleBuilder.runner(projectDir, "build", "--build-cache").build()
            assertThat(build).task(":library:$RESOLVE_TASK_NAME").succeeded()
            assert(dir.listFiles()?.isNotEmpty() == true) {
                "deleting $dir did not make the resolve task fetch it again"
            }

            // Resolving again must not invalidate the products already built.
            val compileOutcome =
                build
                    .task(":library:${compileTaskName(AppleCompileTarget.iosSimulatorArm64)}")
                    ?.outcome
            assert(compileOutcome == TaskOutcome.UP_TO_DATE) {
                "the package was compiled again after $dir came back, it was $compileOutcome"
            }
        }
    }

    private fun cacheFixtureBuilder() =
        SmpKMPTestFixture
            .builder()
            .withBuildPath(testProjectDir.root.absolutePath)
            .withGradleCache()

    /**
     * @param scratchDirs the directories of `scratch/` the cinterop step needs and that must be
     * populated by the cold build, wiped by `clean` and rebuilt by the warm build: `artifacts`
     * for the binary dependencies, `checkouts` for the source ones.
     */
    private fun assertSurvivesBuildCacheHit(
        fixture: SmpKMPTestFixture,
        targets: List<AppleCompileTarget>,
        scratchDirs: List<String> = listOf("artifacts"),
    ) {
        val projectDir = fixture.gradleProject.rootDir
        val restoredDirs = scratchDirs.map { File(projectDir, "$RESOLVE_PATH/$it") }
        val compileTasks = targets.map { ":library:${compileTaskName(it)}" }

        // Given a cold build populating the build cache
        // `--info` makes Gradle log its caching decisions, which are checked below.
        val coldBuild = GradleBuilder.runner(projectDir, "build", "--build-cache", "--info").build()
        assertThat(coldBuild).task(":library:build").succeeded()
        restoredDirs.forEach { dir ->
            assert(dir.listFiles()?.isNotEmpty() == true) { "SwiftPM did not populate $dir" }
        }

        // Storing the entry must work: a symbolic link left in the build directory by SwiftPM
        // is enough to make Gradle fail while packing the output tree.
        val coldDecisions = coldBuild.cacheDecisions(compileTasks)
        println("[cold build] $coldDecisions")
        assert(coldDecisions.stored == compileTasks.toSet()) {
            "every compile task should have been stored in the build cache, got $coldDecisions"
        }

        // When the build directory is wiped, like a fresh clone on CI with a warm cache
        GradleBuilder.runner(projectDir, "clean").build()
        restoredDirs.forEach { dir ->
            assert(!dir.exists()) { "the clean task did not remove $dir" }
        }

        // Then the warm build is served from the cache and still produces a working cinterop
        val warmBuild = GradleBuilder.runner(projectDir, "build", "--build-cache", "--info").build()
        val outcomes =
            targets.associateWith { target ->
                warmBuild.task(":library:${compileTaskName(target)}")?.outcome
            }
        assert(outcomes.values.all { it == TaskOutcome.FROM_CACHE }) {
            "every compile task should have been served from the build cache, got $outcomes"
        }
        val warmDecisions = warmBuild.cacheDecisions(compileTasks)
        println("[warm build] $warmDecisions")
        assert(warmDecisions.loaded == compileTasks.toSet()) {
            "the build log does not report a cache entry loaded for every compile task, " +
                "Gradle reported $warmDecisions"
        }

        // The resolve task is what puts the dependencies back next to the restored products.
        assertThat(warmBuild).task(":library:$RESOLVE_TASK_NAME").succeeded()
        restoredDirs.forEach { dir ->
            assert(dir.listFiles()?.isNotEmpty() == true) {
                "$dir was not resolved again next to the products restored from the cache"
            }
        }

        targets.forEach { target ->
            assertThat(warmBuild).task(":library:${cinteropTaskName(target)}").succeeded()
        }
        assertThat(warmBuild).task(":library:build").succeeded()
    }

    /**
     * The caching decisions Gradle logged, restricted to [tasks].
     *
     * @param stored the tasks whose output was pushed to the build cache.
     * @param loaded the tasks whose output was restored from the build cache.
     * @param disabled the tasks Gradle refused to cache, with the reason it gave.
     */
    private data class CacheDecisions(
        val stored: Set<String>,
        val loaded: Set<String>,
        val disabled: Map<String, String>,
    )

    private fun BuildResult.cacheDecisions(tasks: List<String>): CacheDecisions {
        val lines = output.lines()
        val stored = mutableSetOf<String>()
        val loaded = mutableSetOf<String>()
        val disabled = mutableMapOf<String, String>()
        lines.forEachIndexed { index, line ->
            STORED_ENTRY.find(line)?.let { stored += it.groupValues[1] }
            LOADED_ENTRY.find(line)?.let { loaded += it.groupValues[1] }
            CACHING_DISABLED.find(line)?.let { match ->
                // the reason is logged on the next line, indented
                disabled[match.groupValues[1]] = lines.getOrNull(index + 1).orEmpty().trim()
            }
        }
        return CacheDecisions(
            stored = stored.intersect(tasks.toSet()),
            loaded = loaded.intersect(tasks.toSet()),
            disabled = disabled.filterKeys { it in tasks },
        )
    }

    private companion object {
        const val SCRATCH_PATH = "library/build/spmKmpPlugin/dummy/scratch"

        /**
         * Where the resolved dependencies land.
         *
         * `native` resolves into the scratch directory itself; `swiftbuild` needs a scratch
         * directory per target, so the resolution moves to one they all share.
         */
        val RESOLVE_PATH: String
            get() = if (TestBuildSystem.isSwiftBuild) "$SCRATCH_PATH/shared" else SCRATCH_PATH
        const val RESOLVE_TASK_NAME = "SwiftPackageConfigAppleDummyResolveSwiftPackage"

        val STORED_ENTRY = """Stored cache entry for task '(:[^']+)'""".toRegex()
        val LOADED_ENTRY = """Loaded cache entry for task '(:[^']+)'""".toRegex()
        val CACHING_DISABLED = """Caching disabled for task '(:[^']+)'""".toRegex()

        val REMOTE_BINARY_DEPENDENCY =
            """
            remoteBinary(
                url = URI("https://spmforkmp.eu/DummyFrameworkV2.xcframework.zip"),
                checksum = "90da1dfbf1b52b647958974002a329a60e291b463fcb69a53e2e42b74ead0a94",
                packageName = "DummyFramework",
                exportToKotlin = true
            )
            """.trimIndent()

        fun swiftSource(vararg modules: String) =
            """
            import Foundation
            ${modules.joinToString("\n") { "import $it" }}
            @objc public class MySwiftClass: NSObject {
            }
            """.trimIndent()

        fun compileTaskName(target: AppleCompileTarget) =
            "SwiftPackageConfigAppleDummyCompileSwiftPackage${target.name.titleCase()}"

        fun cinteropTaskName(target: AppleCompileTarget) = "cinteropDummy${target.name.titleCase()}"

        fun String.titleCase() = replaceFirstChar { it.uppercase() }
    }
}
