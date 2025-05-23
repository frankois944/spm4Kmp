package io.github.frankois944.spmForKmp.config

import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.Serializable

/**
 * Represents different compilation targets for Apple platforms, including iOS, watchOS, tvOS, and macOS.
 *
 * Enum entries correspond to specific architectures (e.g., x64, arm64) and environments
 * (e.g., simulator or real device).
 *
 * This class provides utility methods to retrieve target-specific details such as architecture,
 * SDK names, triple identifiers, platform versions, and minimum OS versions.
 *
 * Utility methods:
 * - `triple(version: String)`: Constructs a triple identifier for the target.
 * - `packageBuildDirName()`: Retrieves the build directory identifier specific to the target.
 * - `sdk()`: Determines the SDK name for the target.
 * - `getOsVersion(...)`: Resolves the minimum OS version for the target based on platform-specific inputs.
 * - `linkerPlatformVersionName()`: Retrieves the linker platform version flag for the target.
 * - `linkerMinOsVersionName()`: Retrieves the linker minimum OS version flag for the target.
 *
 * Companion object:
 * - `byKonanName(konanName: String)`: Maps a Konan target name to the corresponding `CompileTarget` enum entry.
 *
 * Internal methods and properties are primarily used for specifying target details
 * in the context of building, linking, or generating compiler-specific configurations.
 */
@Suppress("EnumEntryName", "EnumNaming")
public enum class AppleCompileTarget : Serializable {
    iosX64,
    iosArm64,
    iosSimulatorArm64,
    watchosX64,
    watchosArm64,
    watchosSimulatorArm64,
    tvosX64,
    tvosArm64,
    tvosSimulatorArm64,
    macosX64,
    macosArm64,
    ;

    internal fun triple(version: String) = "${this.arch()}-apple-${this.osCompiler()}$version${this.simulatorSuffix()}"

    internal fun packageBuildDirName() = "${this.arch()}-apple-${this.osCompiler()}${this.simulatorSuffix()}"

    private fun osCompiler(): String =
        when (this) {
            iosX64, iosArm64, iosSimulatorArm64 -> "ios"
            watchosX64, watchosArm64, watchosSimulatorArm64 -> "watchos"
            tvosX64, tvosArm64, tvosSimulatorArm64 -> "tvos"
            macosX64, macosArm64 -> "macosx"
        }

    internal fun xcFrameworkArchName(): String =
        when (this) {
            iosArm64 -> "ios-arm64"
            iosX64, iosSimulatorArm64 -> "ios-arm64_x86_64-simulator"
            watchosArm64 -> "watchos-arm64_arm64_32_armv7k"
            watchosX64, watchosSimulatorArm64 -> "watchos-arm64_x86_64-simulator"
            tvosArm64 -> "tvos-arm64"
            tvosX64, tvosSimulatorArm64 -> "tvos-arm64_x86_64-simulator"
            macosX64, macosArm64 -> "macos-arm64_x86_64"
        }

    internal fun sdk() =
        when (this) {
            iosArm64 -> "iphoneos"
            iosX64, iosSimulatorArm64 -> "iphonesimulator"
            watchosArm64 -> "watchos"
            watchosX64, watchosSimulatorArm64 -> "watchsimulator"
            tvosArm64 -> "appletvos"
            tvosX64, tvosSimulatorArm64 -> "appletvsimulator"
            macosX64, macosArm64 -> "macosx"
        }

    private fun arch() =
        when (this) {
            iosX64 -> "x86_64"
            iosArm64, iosSimulatorArm64 -> "arm64"
            watchosX64 -> "x86_64"
            watchosArm64, watchosSimulatorArm64 -> "arm64"
            tvosX64 -> "x86_64"
            tvosArm64, tvosSimulatorArm64 -> "arm64"
            macosX64 -> "x86_64"
            macosArm64 -> "arm64"
        }

    private val simulatorSuffixValue = "-simulator"

    private fun simulatorSuffix() =
        when (this) {
            iosX64, iosSimulatorArm64 -> simulatorSuffixValue
            watchosX64, watchosSimulatorArm64 -> simulatorSuffixValue
            tvosX64, tvosSimulatorArm64 -> simulatorSuffixValue
            else -> ""
        }

    internal fun getOsVersion(
        minIos: String?,
        minWatchos: String?,
        minTvos: String?,
        minMacos: String?,
    ): String? =
        when (this) {
            iosX64,
            iosArm64,
            iosSimulatorArm64,
            -> minIos

            watchosX64,
            watchosArm64,
            watchosSimulatorArm64,
            -> minWatchos

            tvosX64,
            tvosArm64,
            tvosSimulatorArm64,
            -> minTvos

            macosX64,
            macosArm64,
            -> minMacos
        }

    /**
     * @see [org.jetbrains.kotlin.konan.target.KonanTarget](https://github.com/JetBrains/kotlin/blob/v2.1.0/native/utils/src/org/jetbrains/kotlin/konan/target/KonanTarget.kt)
     */
    internal companion object {
        fun fromKonanTarget(konanName: KonanTarget): AppleCompileTarget? =
            when (konanName) {
                KonanTarget.IOS_X64 -> iosX64
                KonanTarget.IOS_ARM64 -> iosArm64
                KonanTarget.IOS_SIMULATOR_ARM64 -> iosSimulatorArm64
                KonanTarget.WATCHOS_X64 -> watchosX64
                KonanTarget.WATCHOS_ARM64 -> watchosArm64
                KonanTarget.WATCHOS_SIMULATOR_ARM64 -> watchosSimulatorArm64
                KonanTarget.TVOS_X64 -> tvosX64
                KonanTarget.TVOS_ARM64 -> tvosArm64
                KonanTarget.TVOS_SIMULATOR_ARM64 -> tvosSimulatorArm64
                KonanTarget.MACOS_X64 -> macosX64
                KonanTarget.MACOS_ARM64 -> macosArm64
                else -> null
            }

        private const val serialVersionUID: Long = 1
    }
}
