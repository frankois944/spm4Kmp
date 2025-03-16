package io.github.frankois944.spmForKmp.definition.packageSetting.swiftSetting

import java.io.Serializable

internal class SwiftSetting(
    override var defines: List<String> = emptyList(),
    override var unsafeFlags: List<String> = emptyList(),
    override var enableExperimentalFeature: String? = null,
    override var enableUpcomingFeature: String? = null,
    override var swiftLanguageMode: String? = null,
    override var interoperabilityMode: String? = null,
) : SwiftSettingConfig,
    Serializable {
    private companion object {
        private const val serialVersionUID: Long = 1
    }
}
