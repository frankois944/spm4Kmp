package fr.frankois944.spm.kmp.plugin.utils

import fr.frankois944.spm.kmp.plugin.fixture.SmpKMPTestFixture
import org.gradle.internal.cc.base.logger
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.condition.OS
import java.io.File

fun assumeMacos() {
    assumeTrue(OS.MAC.isCurrentOs)
}

fun assumeLinux() {
    assumeTrue(OS.LINUX.isCurrentOs)
}

fun assertPackageResolved(
    fixture: SmpKMPTestFixture,
    vararg packageNames: String,
) {
    val resolvedFile =
        File(
            fixture.gradleProject.rootDir,
            "library/build/spmKmpPlugin/",
        )
    logger.debug(
        """
        Build Dir : ${File(fixture.gradleProject.rootDir, "library/build/spmKmpPlugin/")}
        """.trimIndent(),
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

fun getManifestContent(
    fixture: SmpKMPTestFixture,
    content: (String) -> Unit,
) {
    val resolvedFile =
        File(
            fixture.gradleProject.rootDir,
            "library/build/spmKmpPlugin/input/Package.swift",
        )
    assertTrue(resolvedFile.exists(), "Package.swift file not found")
    content(resolvedFile.readText())
}

fun getPackageResolvedContent(
    fixture: SmpKMPTestFixture,
    content: (String) -> Unit,
) {
    val resolvedFile =
        File(
            fixture.gradleProject.rootDir,
            "library/build/spmKmpPlugin/input/Package.resolved",
        )
    assertTrue(resolvedFile.exists(), "Package.resolved file not found")
    content(resolvedFile.readText())
}
