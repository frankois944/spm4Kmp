package io.github.frankois944.spmForKmp.definition.packageSetting.swiftSetting

import java.io.Serializable

/**
 * [Documentation](https://developer.apple.com/documentation/packagedescription/swiftsetting)
 */
public interface SwiftSettingConfig : Serializable {
    /**
     * Defines a compilation condition.
     */
    public var defines: List<String>

    /**
     * Set unsafe flags to pass arbitrary command-line flags to the corresponding build tool.
     */
    public var unsafeFlags: List<String>

    /**
     * Enable an experimental feature with the given name.
     */
    public var enableExperimentalFeature: String?

    /**
     * Enable an upcoming feature with the given name.
     */
    public var enableUpcomingFeature: String?

    /**
     * Defines a -language-mode to pass to the corresponding build tool.
     */
    public var swiftLanguageMode: String?

    /**
     * Enable Swift interoperability with a given language.
     *
     * C / Cxx
     */
    public var interoperabilityMode: String?
}
