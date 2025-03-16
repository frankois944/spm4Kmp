package io.github.frankois944.spmForKmp.definition.packageSetting

import io.github.frankois944.spmForKmp.definition.packageSetting.cSetting.CSettingConfig
import io.github.frankois944.spmForKmp.definition.packageSetting.cxxSetting.CxxSettingConfig
import io.github.frankois944.spmForKmp.definition.packageSetting.linkerSetting.LinkerSettingConfig
import io.github.frankois944.spmForKmp.definition.packageSetting.swiftSetting.SwiftSettingConfig
import java.io.Serializable

public interface TargetSettingsConfig : Serializable {
    /**
     * The target’s C build settings.
     *
     * [Reference](https://developer.apple.com/documentation/packagedescription/target/csettings).
     */
    public fun cSetting(setting: CSettingConfig.() -> Unit)

    /**
     * The target’s C++ build settings.
     *
     * [Reference](https://developer.apple.com/documentation/packagedescription/target/cxxsettings).
     */
    public fun cxxSetting(setting: CxxSettingConfig.() -> Unit)

    /**
     * The target’s Swift build settings.
     *
     * [Reference](https://developer.apple.com/documentation/packagedescription/target/swiftsettings)
     */
    public fun swiftSettings(setting: SwiftSettingConfig.() -> Unit)

    /**
     * The target’s linker settings.
     *
     * [Reference](https://developer.apple.com/documentation/packagedescription/target/linkersettings)
     */
    public fun linkerSetting(setting: LinkerSettingConfig.() -> Unit)
}
