@file:OptIn(ExperimentalPathApi::class)

package io.github.frankois944.spmForKmp

import com.autonomousapps.kit.GradleBuilder
import com.autonomousapps.kit.truth.TestKitTruth.Companion.assertThat
import io.github.frankois944.spmForKmp.config.AppleCompileTarget
import io.github.frankois944.spmForKmp.fixture.KotlinSource
import io.github.frankois944.spmForKmp.fixture.SmpKMPTestFixture
import io.github.frankois944.spmForKmp.fixture.SwiftSource
import io.github.frankois944.spmForKmp.utils.BaseTest
import io.github.frankois944.spmForKmp.utils.getExportedPackageContent
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteRecursively
import kotlin.io.path.exists

class CopyResourcesTest : BaseTest() {
    @Test
    fun `build with copy resources task`() {
        val localPackageDirectory = File("src/functionalTest/resources/LocalSourceDummyFramework")
        val xcFrameworkDirectory = File("src/functionalTest/resources/DummyFramework.xcframework")
        // Given
        val fixture =
            SmpKMPTestFixture
                .builder()
                .withBuildPath(testProjectDir.root.absolutePath)
                .withTargets(AppleCompileTarget.iosArm64)
                .withRawDependencies(
                    KotlinSource.of(
                        content =
                            """
remotePackageVersion(
    url = URI("https://github.com/firebase/firebase-ios-sdk.git"),
    // Libraries from the package
    products = {
    // Export to Kotlin for use in shared Kotlin code
        add("FirebaseAnalytics", exportToKotlin = true)
    },
    // Package version
    version = "11.8.1",
)
localPackage(
   path = "${localPackageDirectory.absolutePath}",
   products = {
       add(
           ProductName(
               "LocalSourceDummyFramework"
           ),
       )
   },
)
localBinary(
   path = "${xcFrameworkDirectory.absolutePath}",
   packageName = "DummyFramework"
)
remotePackageVersion(
   url = URI("https://github.com/krzyzanowskim/CryptoSwift.git"),
   version = "1.8.3",
   products = {
       add("CryptoSwift")
   },
)
remotePackageVersion(
   url = URI("https://github.com/kishikawakatsumi/KeychainAccess.git"),
   products = {
       add("KeychainAccess")
   },
   version = "4.2.2",
)
remotePackageVersion(
   url = URI("https://github.com/frankois944/QuickServiceLocator"),
   products = {
       add("QuickServiceLocator")
   },
   version = "0.2.0",
)
remotePackageBranch(
   url = URI("https://github.com/FluidGroup/JAYSON"),
   products = {
       add("JAYSON")
   },
   branch = "main",
)
remotePackageCommit(
   url = URI("https://github.com/square/Valet"),
   products = {
       add("Valet")
   },
   revision = "29bea846b29f9880a07dd1828596953d0fd495ce",
)
                            """.trimIndent(),
                    ),
                ).withSwiftSources(
                    SwiftSource.of(
                        content =
                            """
                            import Foundation
                            import CryptoSwift
                            import Valet
                            import JAYSON
                            import QuickServiceLocator
                            import KeychainAccess
                            import FirebaseAnalytics
                            """,
                    ),
                ).build()

        val appBuiltProductDir = "/tmp/resource-test/"
        val appContentFolderPath = "iphone-test"
        val destination = Path(appBuiltProductDir, appContentFolderPath)
        destination.createDirectories()
        val parameters =
            buildList {
                add("SwiftPackageConfigAppleDummyCopyPackageResourcesIosArm64")
                add("-Pio.github.frankois944.spmForKmp.PLATFORM_NAME=iphoneos")
                add("-Pio.github.frankois944.spmForKmp.ARCHS=arm64")
                add("-Pio.github.frankois944.spmForKmp.BUILT_PRODUCTS_DIR=$appBuiltProductDir")
                add("-Pio.github.frankois944.spmForKmp.CONTENTS_FOLDER_PATH=$appContentFolderPath")
            }

        // When I build the project
        val buildTask =
            GradleBuilder
                .runner(
                    fixture.gradleProject.rootDir,
                    "build",
                ).build()

        assertThat(buildTask).task(":library:build").succeeded()

        // And when I run the copy resources task
        val copyResourceTask =
            GradleBuilder
                .runner(
                    fixture.gradleProject.rootDir,
                    *parameters.toTypedArray(),
                ).build()

        // Then
        assertThat(copyResourceTask).task(":library:dummyCopyPackageResources").succeeded()
        assert(
            destination
                .toFile()
                .resolve("Frameworks")
                .listFiles()
                .size == 1,
        ) { "The output folder must not be empty" }
        assert(
            destination
                .toFile()
                .resolve("Frameworks")
                .listFiles { it.nameWithoutExtension == "DummyFramework" }
                .isNotEmpty(),
        ) { "The framework folder must contains DummyFramework" }
        assert(destination.toFile().listFiles().size == 3) { "The output folder must contains 3 folders" }
        assert(!fixture.getExportedPackageContent().contains("CryptoSwift"))
        assert(!fixture.getExportedPackageContent().contains("DummyFramework"))
        assert(!fixture.getExportedPackageContent().contains("Default"))
        assert(!fixture.getExportedPackageContent().contains("Valet"))
        assert(!fixture.getExportedPackageContent().contains("LocalSourceDummyFramework"))
        assert(fixture.getExportedPackageContent().contains("FirebaseAnalytics"))

        if (destination.exists()) {
            destination.deleteRecursively()
        }
    }
}
