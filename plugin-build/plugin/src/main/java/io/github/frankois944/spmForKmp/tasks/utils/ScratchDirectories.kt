package io.github.frankois944.spmForKmp.tasks.utils

import io.github.frankois944.spmForKmp.SPM_ARTIFACTS_DIR_NAME
import io.github.frankois944.spmForKmp.SPM_CHECKOUTS_DIR_NAME
import io.github.frankois944.spmForKmp.SPM_WORKSPACE_STATE_NAME
import io.github.frankois944.spmForKmp.config.AppleCompileTarget
import io.github.frankois944.spmForKmp.config.SpmBuildSystem
import org.gradle.api.logging.Logger
import java.io.File
import java.nio.file.Files

/** Holds what the resolve step produced, shared by every target of the package. */
private const val SHARED_RESOLVE_DIR_NAME = "shared"

/** SwiftPM's own cache of the git repositories it fetched. */
private const val SPM_REPOSITORIES_DIR_NAME = "repositories"

/**
 * What a per-target scratch directory links back to the shared one.
 *
 * Linking all four is what keeps the resolution shared: with only the directories linked, SwiftPM
 * finds no `workspace-state.json` of its own and creates the working copies again for every
 * target, which is most of what resolving costs.
 */
internal val SHARED_RESOLVE_ENTRIES: List<String> =
    listOf(
        SPM_CHECKOUTS_DIR_NAME,
        SPM_REPOSITORIES_DIR_NAME,
        SPM_ARTIFACTS_DIR_NAME,
        SPM_WORKSPACE_STATE_NAME,
    )

/**
 * Where the dependencies are resolved into.
 *
 * `native` resolves and builds in the same directory; `swiftbuild` cannot, see
 * [targetScratchDirectory], so the resolution moves to a directory of its own that every target
 * links to.
 */
internal fun sharedResolveDirectory(
    buildSystem: SpmBuildSystem,
    packageScratchDir: File,
): File =
    when (buildSystem) {
        SpmBuildSystem.NATIVE -> packageScratchDir
        SpmBuildSystem.SWIFTBUILD -> packageScratchDir.resolve(SHARED_RESOLVE_DIR_NAME)
    }

/**
 * Where a target is built.
 *
 * `native` writes one directory per triple inside a single scratch directory, so every target of
 * the package shares it. `swiftbuild` leaves the architecture out of its output paths — it writes
 * to `out/Products/<Config>[-<sdk>]` — so two targets sharing an SDK, such as `iosX64` and
 * `iosSimulatorArm64`, would overwrite each other's static archive. Each target gets its own
 * scratch directory instead.
 *
 * Splitting it also keeps the builds parallel: SwiftPM takes an exclusive lock on the scratch
 * directory, so targets sharing one are serialised even when Gradle runs their tasks at once.
 */
internal fun targetScratchDirectory(
    buildSystem: SpmBuildSystem,
    packageScratchDir: File,
    target: AppleCompileTarget,
): File =
    when (buildSystem) {
        SpmBuildSystem.NATIVE -> packageScratchDir
        SpmBuildSystem.SWIFTBUILD -> packageScratchDir.resolve(target.name)
    }

/**
 * Points a per-target scratch directory at the shared resolution.
 *
 * Does nothing when the two are the same directory, which is the case for `native`.
 */
internal fun linkSharedResolveEntries(
    targetScratchDir: File,
    sharedResolveDir: File,
    logger: Logger,
) {
    if (targetScratchDir.canonicalFile == sharedResolveDir.canonicalFile) return
    targetScratchDir.mkdirs()
    SHARED_RESOLVE_ENTRIES.forEach { entry ->
        val link = targetScratchDir.resolve(entry).toPath()
        val destination = sharedResolveDir.resolve(entry).toPath()
        if (Files.isSymbolicLink(link)) {
            if (Files.readSymbolicLink(link) == destination) return@forEach
            Files.delete(link)
        } else if (Files.exists(link)) {
            // a real directory left by an earlier build, before the resolution was shared
            link.toFile().deleteRecursively()
        }
        logger.debug("Link {} to the shared resolve directory {}", link, destination)
        Files.createSymbolicLink(link, destination)
    }
}
