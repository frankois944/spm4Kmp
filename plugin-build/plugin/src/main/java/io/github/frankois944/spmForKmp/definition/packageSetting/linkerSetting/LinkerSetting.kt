package io.github.frankois944.spmForKmp.definition.packageSetting.linkerSetting

import java.io.Serializable

/**
 * [Documentation](https://developer.apple.com/documentation/packagedescription/linkersetting)
 */
internal data class LinkerSetting(
    /**
     * Declares linkage to a system framework.
     */
    override var linkedFramework: List<String> = emptyList(),
    /**
     * Declares linkage to a system library.
     */
    override var linkedLibrary: List<String> = emptyList(),
    /**
     * Sets unsafe flags to pass arbitrary command-line flags to the corresponding build tool.
     */
    override var unsafeFlags: List<String> = emptyList(),
) : LinkerSettingConfig,
    Serializable {
    private companion object {
        private const val serialVersionUID: Long = 1
    }
}
