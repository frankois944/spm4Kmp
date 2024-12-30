package fr.frankois944.spm.kmp.plugin

import org.jetbrains.kotlin.konan.target.KonanTarget

@Suppress("EnumEntryName")
public enum class CompileTarget {
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

    internal fun getTriple(version: String): String = "${this.arch()}-apple-${this.osCompiler()}$version${this.simulatorSuffix()}"

    internal fun getPackageBuildDir(): String = "${this.arch()}-apple-${this.osCompiler()}${this.simulatorSuffix()}"

    private fun osCompiler(): String =
        when (this) {
            iosX64, iosArm64, iosSimulatorArm64 -> "ios"
            watchosX64, watchosArm64, watchosSimulatorArm64 -> "watchos"
            tvosX64, tvosArm64, tvosSimulatorArm64 -> "tvos"
            macosX64, macosArm64 -> "macosx"
        }

    internal fun sdk() =
        when (this) {
            iosX64 -> "iphonesimulator"
            iosArm64 -> "iphoneos"
            iosSimulatorArm64 -> "iphonesimulator"
            watchosX64 -> "watchsimulator"
            watchosArm64 -> "watchos"
            watchosSimulatorArm64 -> "watchsimulator"
            tvosX64 -> "appletvsimulator"
            tvosArm64 -> "appletvos"
            tvosSimulatorArm64 -> "appletvsimulator"
            macosX64 -> "macosx"
            macosArm64 -> "macosx"
        }

    private fun arch() =
        when (this) {
            iosX64 -> "x86_64"
            iosArm64 -> "arm64"
            iosSimulatorArm64 -> "arm64"
            watchosX64 -> "x86_64"
            watchosArm64 -> "arm64"
            watchosSimulatorArm64 -> "arm64"
            tvosX64 -> "x86_64"
            tvosArm64 -> "arm64"
            tvosSimulatorArm64 -> "arm64"
            macosX64 -> "x86_64"
            macosArm64 -> "arm64"
        }

    private fun simulatorSuffix() =
        when (this) {
            iosX64 -> "-simulator"
            iosArm64 -> ""
            iosSimulatorArm64 -> "-simulator"
            watchosX64 -> "-simulator"
            watchosArm64 -> ""
            watchosSimulatorArm64 -> "-simulator"
            tvosX64 -> "-simulator"
            tvosArm64 -> ""
            tvosSimulatorArm64 -> "-simulator"
            macosX64 -> ""
            macosArm64 -> ""
        }

    internal fun getOsVersion(
        minIos: String,
        minWatchos: String,
        minTvos: String,
        minMacos: String,
    ): String =
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

    internal fun linkerPlatformVersionName() =
        when (this) {
            iosArm64 -> "platform_version ios"
            iosX64, iosSimulatorArm64 -> "platform_version ios-simulator"
            watchosArm64 -> "platform_version watchos"
            watchosX64, watchosSimulatorArm64 -> "platform_version watchos-simulator"
            tvosArm64 -> "platform_version tvos"
            tvosX64, tvosSimulatorArm64 -> "platform_version tvos-simulator"
            macosX64, macosArm64 -> "platform_version macosx"
        }

    internal fun linkerMinOsVersionName() =
        when (this) {
            iosX64 -> "ios_simulator_version_min"
            iosArm64 -> "ios_version_min"
            iosSimulatorArm64 -> "ios_simulator_version_min"
            watchosX64 -> "watchos_simulator_version_min"
            watchosArm64 -> "watchos_version_min"
            watchosSimulatorArm64 -> "watchos_simulator_version_min"
            tvosX64 -> "tvos_simulator_version_min"
            tvosArm64 -> "tvos_version_min"
            tvosSimulatorArm64 -> "tvos_simulator_version_min"
            macosX64 -> "macosx_version_min"
            macosArm64 -> "macosx_version_min"
        }

    /**
     * @see [KonanTarget](https://github.com/JetBrains/kotlin/blob/v2.1.0/native/utils/src/org/jetbrains/kotlin/konan/target/KonanTarget.kt)
     */
    internal companion object {
        fun byKonanName(konanName: String): CompileTarget? =
            when (konanName) {
                KonanTarget.IOS_X64.name -> iosX64
                KonanTarget.IOS_ARM64.name -> iosArm64
                KonanTarget.IOS_SIMULATOR_ARM64.name -> iosSimulatorArm64
                KonanTarget.WATCHOS_X64.name -> watchosX64
                KonanTarget.WATCHOS_ARM64.name -> watchosArm64
                KonanTarget.WATCHOS_SIMULATOR_ARM64.name -> watchosSimulatorArm64
                KonanTarget.TVOS_X64.name -> tvosX64
                KonanTarget.TVOS_ARM64.name -> tvosArm64
                KonanTarget.TVOS_SIMULATOR_ARM64.name -> tvosSimulatorArm64
                KonanTarget.MACOS_X64.name -> macosX64
                KonanTarget.MACOS_ARM64.name -> macosArm64
                else -> null
            }
    }
}
