package io.github.frankois944.spmForKmp.definition.packageSetting.cxxSetting

import java.io.Serializable

internal class CxxSetting(
    override var defines: List<Pair<String, String?>> = emptyList(),
    override var headerSearchPath: List<String> = emptyList(),
    override var unsafeFlags: List<String> = emptyList(),
) : CxxSettingConfig,
    Serializable {
    private companion object {
        private const val serialVersionUID: Long = 1
    }
}
