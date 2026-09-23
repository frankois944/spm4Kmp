package io.github.frankois944.spmForKmp.tasks.utils

import io.github.frankois944.spmForKmp.config.AppleCompileTarget
import io.github.frankois944.spmForKmp.config.SpmBuildSystem
import org.gradle.api.logging.Logging
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScratchDirectoriesTest {
    private val logger = Logging.getLogger("ScratchDirectoriesTest")
    private val scratch = File("/tmp/scratch")

    @Test
    fun `native resolves and builds every target in the same directory`() {
        assertEquals(scratch, sharedResolveDirectory(SpmBuildSystem.NATIVE, scratch))
        AppleCompileTarget.entries.forEach { target ->
            assertEquals(scratch, targetScratchDirectory(SpmBuildSystem.NATIVE, scratch, target))
        }
    }

    @Test
    fun `swiftbuild keeps the resolution apart from the builds`() {
        assertEquals(
            File("/tmp/scratch/shared"),
            sharedResolveDirectory(SpmBuildSystem.SWIFTBUILD, scratch),
        )
        assertEquals(
            File("/tmp/scratch/iosSimulatorArm64"),
            targetScratchDirectory(SpmBuildSystem.SWIFTBUILD, scratch, AppleCompileTarget.iosSimulatorArm64),
        )
    }

    @Test
    fun `swiftbuild gives every target its own scratch directory`() {
        val directories =
            AppleCompileTarget.entries.map {
                targetScratchDirectory(SpmBuildSystem.SWIFTBUILD, scratch, it)
            }
        assertEquals(AppleCompileTarget.entries.size, directories.toSet().size)
    }

    /**
     * The point of the split: `swiftbuild` leaves the architecture out of its output paths, so
     * these two targets resolve to the same directory inside one scratch directory. Giving each
     * a scratch directory of its own is what stops them overwriting each other's static archive.
     */
    @Test
    fun `targets sharing an sdk no longer share a build directory under swiftbuild`() {
        val colliding =
            listOf(
                AppleCompileTarget.iosX64 to AppleCompileTarget.iosSimulatorArm64,
                AppleCompileTarget.watchosX64 to AppleCompileTarget.watchosSimulatorArm64,
                AppleCompileTarget.watchosArm64 to AppleCompileTarget.watchosArm32,
                AppleCompileTarget.tvosX64 to AppleCompileTarget.tvosSimulatorArm64,
                AppleCompileTarget.macosX64 to AppleCompileTarget.macosArm64,
            )
        colliding.forEach { (left, right) ->
            assertEquals(
                left.sdk(),
                right.sdk(),
                "$left and $right are only a collision risk because they share an SDK",
            )
            val leftDir = getTargetBuildDirectory(SpmBuildSystem.SWIFTBUILD, scratch, left, "release")
            val rightDir = getTargetBuildDirectory(SpmBuildSystem.SWIFTBUILD, scratch, right, "release")
            assertTrue(leftDir != rightDir, "$left and $right still build into $leftDir")
        }
    }

    @Test
    fun `native keeps its per triple build directories inside one scratch directory`() {
        val dir =
            getTargetBuildDirectory(
                SpmBuildSystem.NATIVE,
                scratch,
                AppleCompileTarget.iosSimulatorArm64,
                "release",
            )
        assertEquals(File("/tmp/scratch/arm64-apple-ios-simulator/release"), dir)
    }

    @Test
    fun `linking points the target scratch directory at the shared resolution`() {
        val root = Files.createTempDirectory("scratch-link").toFile()
        val shared = root.resolve("shared").apply { mkdirs() }
        SHARED_RESOLVE_ENTRIES.forEach { shared.resolve(it).mkdirs() }
        val target = root.resolve("iosArm64")

        linkSharedResolveEntries(target, shared, logger)

        SHARED_RESOLVE_ENTRIES.forEach { entry ->
            val link = target.resolve(entry).toPath()
            assertTrue(Files.isSymbolicLink(link), "$entry is not a symbolic link")
            assertEquals(shared.resolve(entry).toPath(), Files.readSymbolicLink(link))
        }
    }

    @Test
    fun `linking is repeatable and replaces a real directory left by an earlier build`() {
        val root = Files.createTempDirectory("scratch-relink").toFile()
        val shared = root.resolve("shared").apply { mkdirs() }
        SHARED_RESOLVE_ENTRIES.forEach { shared.resolve(it).mkdirs() }
        val target = root.resolve("iosArm64")
        // an earlier build resolved into the target directory itself
        target.resolve("checkouts").mkdirs()
        target.resolve("checkouts/Stale").mkdirs()

        linkSharedResolveEntries(target, shared, logger)
        linkSharedResolveEntries(target, shared, logger)

        val link = target.resolve("checkouts").toPath()
        assertTrue(Files.isSymbolicLink(link))
        assertEquals(shared.resolve("checkouts").toPath(), Files.readSymbolicLink(link))
    }

    @Test
    fun `linking does nothing when the two directories are the same`() {
        val root = Files.createTempDirectory("scratch-same").toFile()

        linkSharedResolveEntries(root, root, logger)

        assertEquals(emptyList(), root.listFiles()?.toList().orEmpty())
    }
}
