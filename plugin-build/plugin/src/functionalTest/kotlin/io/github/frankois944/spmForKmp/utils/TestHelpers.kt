package io.github.frankois944.spmForKmp.utils

import io.github.frankois944.spmForKmp.fixture.SmpKMPTestFixture
import java.io.File

internal fun SmpKMPTestFixture.assertPackageResolved(vararg packageNames: String) {
    val resolvedFile =
        File(
            this.gradleProject.rootDir,
            "library/build/spmKmpPlugin/dummy/Package.resolved",
        )
    assert(resolvedFile.exists()) { "Package.resolved file not found" }
    val content = getPackageResolvedContent()
    packageNames.forEach { packageName ->
        assert(
            content.contains("\"identity\" : \"$packageName\"", ignoreCase = true),
        ) { "$packageName dependency not found" }
    }
}

internal fun SmpKMPTestFixture.getManifestContent(): String {
    val resolvedFile =
        File(
            this.gradleProject.rootDir,
            "library/build/spmKmpPlugin/dummy/Package.swift",
        )
    assert(resolvedFile.exists()) { "Package.swift file not found" }
    return resolvedFile.readText()
}

internal fun SmpKMPTestFixture.getPackageResolvedContent(): String {
    val resolvedFile =
        File(
            this.gradleProject.rootDir,
            "library/build/spmKmpPlugin/dummy/Package.resolved",
        )
    assert(resolvedFile.exists()) { "Package.resolved file not found" }
    return resolvedFile.readText()
}

internal fun SmpKMPTestFixture.getExportedPackageContent(): String {
    val resolvedFile =
        File(
            this.gradleProject.rootDir,
            "library/exportedDummy/Package.swift",
        )
    assert(resolvedFile.exists()) { "ExportedPackage Package.swift file not found" }
    return resolvedFile.readText()
}

internal fun SmpKMPTestFixture.getPackageSourceFiles(): Array<out File> {
    val resolvedFile =
        File(
            this.gradleProject.rootDir,
            "library/build/spmKmpPlugin/dummy/Package.swift",
        )
    assert(resolvedFile.exists()) { "Package.swift file not found" }
    return requireNotNull(resolvedFile.listFiles()) { "No source files found found" }
}
