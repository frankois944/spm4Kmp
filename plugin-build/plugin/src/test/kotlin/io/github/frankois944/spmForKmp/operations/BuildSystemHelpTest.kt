package io.github.frankois944.spmForKmp.operations

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Which engines `swift build --build-system` accepts changed between Swift releases, and the
 * plugin has to ask rather than assume: asking for `swiftbuild` on a toolchain that only knows
 * `native`/`xcode` makes SwiftPM reject the argument.
 *
 * The help output below is quoted from the real toolchains.
 */
class BuildSystemHelpTest {
    @Test
    fun `reads the engines of a Swift 6-4 toolchain`() {
        val help =
            """
            BUILD OPTIONS:
              -c, --configuration <configuration>
              --build-system <build-system>
                                      Specify the build system to use. (default: swiftbuild)
                    native            - Native Build System (deprecated)
                    swiftbuild        - Swift Build build engine (default)
                    xcode             - Xcode build system integration (deprecated)
              -debug-info-format <debug-info-format>
            """.trimIndent()

        assertEquals(setOf("native", "swiftbuild", "xcode"), parseBuildSystems(help))
    }

    /** Swift 6.0 to 6.3: no `swiftbuild`, so requesting it must not be attempted. */
    @Test
    fun `reads the engines of an older toolchain that has no swiftbuild`() {
        val help =
            """
            BUILD OPTIONS:
              --build-system <build-system>
                                      Specify the build system to use. (default: native)
                    native            - Native Build System
                    xcode             - Xcode build system integration
              -j, --jobs <jobs>
            """.trimIndent()

        assertEquals(setOf("native", "xcode"), parseBuildSystems(help))
    }

    @Test
    fun `reports nothing for a toolchain without the flag`() {
        val help =
            """
            BUILD OPTIONS:
              -c, --configuration <configuration>
              -j, --jobs <jobs>       The number of jobs to spawn in parallel.
            """.trimIndent()

        assertEquals(emptySet(), parseBuildSystems(help))
    }

    /**
     * Only the block belonging to `--build-system` is read: an engine name mentioned in another
     * option's description must not be picked up.
     */
    @Test
    fun `ignores engine names outside the build-system block`() {
        val help =
            """
            BUILD OPTIONS:
              --build-system <build-system>
                                      Specify the build system to use.
                    native            - Native Build System
              --use-integrated-swift-driver
                    swiftbuild        - not an engine of --build-system
            """.trimIndent()

        assertEquals(setOf("native"), parseBuildSystems(help))
    }
}
