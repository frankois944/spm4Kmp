package io.github.frankois944.spmForKmp.config

import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.Serializable

@Suppress("EnumEntryName", "EnumNaming")
public enum class LinuxCompileTarget : Serializable {
    linuxX64,
    linuxArm64,
    ;

    internal fun getTriple(version: String): String = "${this.arch()}-linux-${this.osCompiler()}$version}"

    internal fun getPackageBuildDir(): String = "${this.arch()}-linux-${this.osCompiler()}"

    private fun osCompiler(): String =
        when (this) {
            linuxX64, linuxArm64 -> "linux"
        }

    internal fun sdk() =
        when (this) {
            linuxX64, linuxArm64 -> "linux"
        }

    private fun arch() =
        when (this) {
            linuxX64 -> "x86_64"
            linuxArm64 -> "arm64"
        }

    /**
     * @see [org.jetbrains.kotlin.konan.target.KonanTarget](https://github.com/JetBrains/kotlin/blob/v2.1.0/native/utils/src/org/jetbrains/kotlin/konan/target/KonanTarget.kt)
     */
    internal companion object {
        fun byKonanName(konanName: String): LinuxCompileTarget? =
            when (konanName) {
                KonanTarget.LINUX_X64.name -> linuxX64
                KonanTarget.LINUX_ARM64.name -> linuxArm64
                else -> null
            }

        private const val serialVersionUID: Long = 1
    }
}
