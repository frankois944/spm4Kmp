package io.github.frankois944.spmForKmp.config

/**
 * The build engine SwiftPM compiles the package with, `swift build --build-system`.
 *
 * The two lay their output out completely differently, so the plugin reads whichever one built
 * the package through a layout of its own; see the `SpmBuildLayout` in `tasks.utils`.
 */
public enum class SpmBuildSystem {
    /**
     * The build system SwiftPM used up to Swift 6.3, and still supports.
     *
     * Deprecated upstream since Swift 6.4 and slated for removal, but it is what the plugin has
     * always built with, and it remains the default until the `swiftbuild` path has been proven
     * against the whole test suite.
     */
    NATIVE,

    /**
     * The Swift Build engine, which SwiftPM defaults to from Swift 6.4 (Xcode 27).
     *
     * Requires a toolchain that understands `--build-system`; on older ones the plugin falls
     * back to [NATIVE].
     */
    SWIFTBUILD,
    ;

    /** The value to pass to `swift build --build-system`. */
    internal fun flagValue(): String = name.lowercase()
}
