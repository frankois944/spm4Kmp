@file:OptIn(ExperimentalPathApi::class)

package io.github.frankois944.spmForKmp

import com.autonomousapps.kit.GradleBuilder
import com.autonomousapps.kit.truth.TestKitTruth.Companion.assertThat
import io.github.frankois944.spmForKmp.config.AppleCompileTarget
import io.github.frankois944.spmForKmp.definition.SwiftDependency
import io.github.frankois944.spmForKmp.fixture.SmpKMPTestFixture
import io.github.frankois944.spmForKmp.utils.BaseTest
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
                .withCopyDependenciesToApp(true)
                .withDependencies(
                    buildList {
                        add(
                            SwiftDependency.Package.Local(
                                path = localPackageDirectory.absolutePath,
                                products = {
                                    add("LocalSourceDummyFramework")
                                },
                            ),
                        )
                        add(
                            SwiftDependency.Binary.Local(
                                path = xcFrameworkDirectory.absolutePath,
                                packageName = "DummyFramework",
                                exportToKotlin = true,
                            ),
                        )
                    },
                ).build()

        val appBuiltProductDir = "/tmp/resource-test/"
        val appContentFolderPath = "iphone-test"
        val destination = Path(appBuiltProductDir, appContentFolderPath)
        destination.createDirectories()
        val parameters =
            buildList {
                add("copyPackageResourcesDummy")
                add("-PPLATFORM_NAME=iphone")
                add("-PARCHS=arm64")
                add("-PBUILT_PRODUCTS_DIR=$appBuiltProductDir")
                add("-PCONTENTS_FOLDER_PATH=$appContentFolderPath")
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
        assertThat(copyResourceTask).task(":library:copyPackageResourcesDummy").succeeded()
        assert(
            destination
                .toFile()
                .resolve("Frameworks")
                .listFiles()
                .isNotEmpty(),
        ) { "The output folder must not be empty" }
        assert(destination.toFile().listFiles().isNotEmpty()) { "The output folder must not be empty" }
        if (destination.exists()) {
            destination.deleteRecursively()
        }
    }
}
