package io.github.frankois944.spmForKmp.definition.packageSetting

import io.github.frankois944.spmForKmp.definition.packageSetting.cSetting.CSetting
import io.github.frankois944.spmForKmp.definition.packageSetting.cSetting.CSettingConfig
import io.github.frankois944.spmForKmp.definition.packageSetting.cxxSetting.CxxSetting
import io.github.frankois944.spmForKmp.definition.packageSetting.cxxSetting.CxxSettingConfig
import io.github.frankois944.spmForKmp.definition.packageSetting.linkerSetting.LinkerSetting
import io.github.frankois944.spmForKmp.definition.packageSetting.linkerSetting.LinkerSettingConfig
import io.github.frankois944.spmForKmp.definition.packageSetting.swiftSetting.SwiftSetting
import io.github.frankois944.spmForKmp.definition.packageSetting.swiftSetting.SwiftSettingConfig
import java.io.Serializable

internal data class TargetSettings(
    var cSettings: CSettingConfig = CSetting(),
    var cxxSettings: CxxSettingConfig = CxxSetting(),
    var swiftSettings: SwiftSettingConfig = SwiftSetting(),
    var linkerSettings: LinkerSettingConfig = LinkerSetting(),
) : Serializable,
    TargetSettingsConfig {
    override fun cSetting(setting: CSettingConfig.() -> Unit) {
        cSettings.apply(setting)
    }

    override fun cxxSetting(setting: CxxSettingConfig.() -> Unit) {
        cxxSettings.apply(setting)
    }

    override fun swiftSettings(setting: SwiftSettingConfig.() -> Unit) {
        swiftSettings.apply(setting)
    }

    override fun linkerSetting(setting: LinkerSettingConfig.() -> Unit) {
        linkerSettings.apply(setting)
    }

    internal companion object {
        private const val serialVersionUID: Long = 1
    }
}
