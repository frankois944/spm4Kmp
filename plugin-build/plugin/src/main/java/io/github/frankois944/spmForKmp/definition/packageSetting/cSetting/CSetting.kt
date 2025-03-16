package io.github.frankois944.spmForKmp.definition.packageSetting.cSetting

import java.io.Serializable

internal class CSetting(
    override var defines: List<Pair<String, String?>> = emptyList(),
    override var headerSearchPath: List<String> = emptyList(),
    override var unsafeFlags: List<String> = emptyList(),
) : CSettingConfig,
    Serializable {
    private companion object {
        private const val serialVersionUID: Long = 1
    }
}
