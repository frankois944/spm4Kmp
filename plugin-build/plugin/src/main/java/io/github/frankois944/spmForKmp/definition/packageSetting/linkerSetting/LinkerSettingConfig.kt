package io.github.frankois944.spmForKmp.definition.packageSetting.linkerSetting

import java.io.Serializable

/**
 * [Documentation](https://developer.apple.com/documentation/packagedescription/linkersetting)
 */
public interface LinkerSettingConfig : Serializable {
    /**
     * Declares linkage to a system framework.
     */
    public var linkedFramework: List<String>

    /**
     * Declares linkage to a system library.
     */
    public var linkedLibrary: List<String>

    /**
     * Sets unsafe flags to pass arbitrary command-line flags to the corresponding build tool.
     */
    public var unsafeFlags: List<String>
}
