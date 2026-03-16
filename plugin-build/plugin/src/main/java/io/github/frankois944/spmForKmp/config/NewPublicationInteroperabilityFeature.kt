@file:Suppress("ktlint:standard:filename")

package io.github.frankois944.spmForKmp.config

internal object NewPublicationInteroperabilityFeature {
    fun extraOpts() = listOf("-Xccall-mode", "direct")

    fun minKotlinVersion() = "2.3.20"
}
