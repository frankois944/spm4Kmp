package io.github.frankois944.spmForKmp.definition.packageSetting.cSetting

import java.io.Serializable

/**
 * [Documentation](https://developer.apple.com/documentation/packagedescription/csetting)
 */
public interface CSettingConfig : Serializable {
    /**
     * Defines a value for a macro.
     */
    public var defines: List<Pair<String, String?>>

    /**
     * Provides a header search path relative to the target’s directory.
     */
    public var headerSearchPath: List<String>

    /**
     * Sets unsafe flags to pass arbitrary command-line flags to the corresponding build tool.
     */
    public var unsafeFlags: List<String>
}
