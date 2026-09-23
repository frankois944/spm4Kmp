package io.github.frankois944.spmForKmp.tasks.utils

import io.github.frankois944.spmForKmp.config.AppleCompileTarget
import io.github.frankois944.spmForKmp.config.SpmBuildSystem
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * The paths asserted here were read off real scratch directories produced by Swift 6.4
 * (Xcode 27), for both build systems.
 */
class SpmBuildLayoutTest {
    private val scratch = File("/tmp/scratch")

    private fun layoutOf(
        buildSystem: SpmBuildSystem,
        target: AppleCompileTarget,
        buildMode: String = "release",
        root: File = scratch,
    ) = spmBuildLayout(buildSystem, root, target, buildMode)

    @Test
    fun `native puts the products in a per triple directory`() {
        assertEquals(
            File("/tmp/scratch/arm64-apple-ios-simulator/release"),
            layoutOf(SpmBuildSystem.NATIVE, AppleCompileTarget.iosSimulatorArm64).productsDirectory,
        )
        assertEquals(
            File("/tmp/scratch/arm64-apple-ios/debug"),
            layoutOf(SpmBuildSystem.NATIVE, AppleCompileTarget.iosArm64, "debug").productsDirectory,
        )
    }

    @Test
    fun `swiftbuild puts the products in a per configuration and sdk directory`() {
        assertEquals(
            File("/tmp/scratch/out/Products/Release-iphonesimulator"),
            layoutOf(SpmBuildSystem.SWIFTBUILD, AppleCompileTarget.iosSimulatorArm64).productsDirectory,
        )
        assertEquals(
            File("/tmp/scratch/out/Products/Debug-iphoneos"),
            layoutOf(SpmBuildSystem.SWIFTBUILD, AppleCompileTarget.iosArm64, "debug").productsDirectory,
        )
    }

    @Test
    fun `swiftbuild omits the sdk suffix for macOS`() {
        assertEquals(
            File("/tmp/scratch/out/Products/Release"),
            layoutOf(SpmBuildSystem.SWIFTBUILD, AppleCompileTarget.macosArm64).productsDirectory,
        )
    }

    /**
     * The architecture appears nowhere in the swiftbuild paths, which is why targets sharing an
     * SDK cannot share a scratch directory.
     */
    @Test
    fun `swiftbuild targets sharing an sdk collide on the same products directory`() {
        assertEquals(
            layoutOf(SpmBuildSystem.SWIFTBUILD, AppleCompileTarget.iosX64).productsDirectory,
            layoutOf(SpmBuildSystem.SWIFTBUILD, AppleCompileTarget.iosSimulatorArm64).productsDirectory,
        )
        // native keeps them apart
        assertTrue(
            layoutOf(SpmBuildSystem.NATIVE, AppleCompileTarget.iosX64).productsDirectory !=
                layoutOf(SpmBuildSystem.NATIVE, AppleCompileTarget.iosSimulatorArm64).productsDirectory,
        )
    }

    @Test
    fun `native discovers modules from the build directories`() {
        val root = Files.createTempDirectory("native-layout").toFile()
        val products = root.resolve("arm64-apple-ios-simulator/release")
        products.resolve("CryptoSwift.build/include").mkdirs()
        products.resolve("CryptoSwift.build/include/module.modulemap").writeText("module CryptoSwift {}")
        products.resolve("DummyFramework.framework/Modules").mkdirs()
        products.resolve("DummyFramework.framework/Modules/module.modulemap").writeText("framework module D {}")

        val modules =
            layoutOf(SpmBuildSystem.NATIVE, AppleCompileTarget.iosSimulatorArm64, root = root)
                .builtModules()
                .associateBy { it.name }

        assertEquals(setOf("CryptoSwift", "DummyFramework"), modules.keys)
        assertEquals(false, modules.getValue("CryptoSwift").isFramework)
        assertNotNull(modules.getValue("CryptoSwift").moduleMap)
        assertEquals(true, modules.getValue("DummyFramework").isFramework)
        assertNotNull(modules.getValue("DummyFramework").moduleMap)
    }

    @Test
    fun `swiftbuild discovers modules from the generated module maps`() {
        val root = Files.createTempDirectory("swiftbuild-layout").toFile()
        val maps = root.resolve("out/Intermediates.noindex/GeneratedModuleMaps-iphonesimulator")
        maps.mkdirs()
        maps.resolve("CryptoSwift.modulemap").writeText("module CryptoSwift {}")
        maps.resolve("CryptoSwift-Swift.h").writeText("")
        maps.resolve("DummyBridge.modulemap").writeText("module DummyBridge {}")
        root.resolve("out/Products/Release-iphonesimulator").mkdirs()

        val modules =
            layoutOf(SpmBuildSystem.SWIFTBUILD, AppleCompileTarget.iosSimulatorArm64, root = root)
                .builtModules()
                .associateBy { it.name }

        assertEquals(setOf("CryptoSwift", "DummyBridge"), modules.keys)
        val cryptoSwift = modules.getValue("CryptoSwift")
        assertEquals(false, cryptoSwift.isFramework)
        assertEquals(maps.resolve("CryptoSwift.modulemap"), cryptoSwift.moduleMap)
        // the generated -Swift.h headers sit next to the module maps
        assertEquals(listOf(maps), cryptoSwift.headerSearchPaths)
    }

    /**
     * The generated definitions reference the module maps by absolute path, and under
     * `swiftbuild` those sit outside the products directory. A compile task that did not declare
     * them would restore a build cache entry without them, leaving cinterop pointing at nothing.
     */
    @Test
    fun `swiftbuild declares the generated module maps as a compile output`() {
        val outputs = layoutOf(SpmBuildSystem.SWIFTBUILD, AppleCompileTarget.iosSimulatorArm64).compileOutputDirectories()

        assertEquals(
            listOf(
                File("/tmp/scratch/out/Products/Release-iphonesimulator"),
                File("/tmp/scratch/out/Intermediates.noindex/GeneratedModuleMaps-iphonesimulator"),
            ),
            outputs,
        )
    }

    @Test
    fun `native keeps everything inside the build directory`() {
        val outputs = layoutOf(SpmBuildSystem.NATIVE, AppleCompileTarget.iosSimulatorArm64).compileOutputDirectories()

        assertEquals(listOf(File("/tmp/scratch/arm64-apple-ios-simulator/release")), outputs)
    }

    @Test
    fun `a missing build directory is reported rather than returning nothing`() {
        assertFailsWith<SpmBuildLayoutException> {
            layoutOf(SpmBuildSystem.NATIVE, AppleCompileTarget.iosArm64).builtModules()
        }
        assertFailsWith<SpmBuildLayoutException> {
            layoutOf(SpmBuildSystem.SWIFTBUILD, AppleCompileTarget.iosArm64).builtModules()
        }
    }
}
