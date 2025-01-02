package io.github.frankois944.spmForKmp.definition

import java.io.Serializable

/**
 * Represents a Swift dependency within a Kotlin Multiplatform project. This sealed interface serves as the base type
 * for various Swift dependency configurations, including binary and package dependencies.
 * Dependencies can be local or remote.
 *
 * @property packageName The name of the package associated with the dependency.
 * This is utilized for identification within
 * the Swift package ecosystem.
 * @property exportToKotlin A flag indicating whether the dependency's API should be exported to Kotlin code.
 * By default, this is set to `false`.
 */
public sealed interface SwiftDependency : Serializable {
    public val packageName: String

    public val exportToKotlin: Boolean

    public sealed interface Binary : SwiftDependency {
        /**
         * Represents a local binary dependency in the Kotlin Multiplatform project.
         *
         * @property path The absolute path to the local xcframework dependency.
         * @property packageName The name of the package associated with this binary.
         * @property exportToKotlin Defines whether the dependency should be exported for use in Kotlin code.
         */
        public data class Local(
            // must be an absolute path
            val path: String,
            override val packageName: String,
            override val exportToKotlin: Boolean = false,
        ) : Binary

        /**
         * Represents a remote binary dependency in a Kotlin Multiplatform project.
         *
         * @property url The URL pointing to the remote binary artifact.
         * @property packageName The name of the package associated with this binary dependency.
         * @property exportToKotlin Defines whether this dependency should be exported for use in Kotlin code.
         * @property checksum The checksum of the remote binary to verify its integrity.
         */
        public data class Remote(
            val url: String,
            override val packageName: String,
            override val exportToKotlin: Boolean = false,
            val checksum: String,
        ) : Binary
    }

    public sealed interface Package : SwiftDependency {
        public val names: List<String>

        /**
         * Represents a local Swift package dependency.
         *
         * @property path The file path to the local Swift package within the project.
         * @property packageName The name of the Swift package.
         * @property names A list of alternative names for the package. Defaults to a list containing the package name.
         * @property exportToKotlin Whether this package should be exported to Kotlin code. Defaults to `false`.
         */
        public data class Local(
            val path: String,
            override val packageName: String,
            override val names: List<String> = listOf(packageName),
            override val exportToKotlin: Boolean = false,
        ) : Package

        public sealed interface Remote : Package {
            public val url: String

            /**
             * Represents a specific version of a remote Swift package to be used
             * as a dependency in a Kotlin Multiplatform project.
             *
             * @property url The URL of the remote Swift package repository.
             * @property names A list of possible names for the package used during dependency configuration.
             * @property packageName The default name of the package, from the first entry in `names` by default.
             * @property exportToKotlin A flag indicating whether the package should be exported to Kotlin.
             * @property version The specific version of the Swift package to be imported.
             */
            public data class Version(
                public override val url: String,
                public override val names: List<String>,
                public override val packageName: String = names.first(),
                override val exportToKotlin: Boolean = false,
                public val version: String,
            ) : Remote

            /**
             * Represents a branch-based remote Swift dependency in a Kotlin Multiplatform project.
             *
             * @property url The URL of the remote Git repository where the package is hosted.
             * @property names A list of name components identifying the package.
             * Typically, this includes the package name.
             * @property packageName The name of the package being declared.
             * Defaults to the first name in the `names` list.
             * @property exportToKotlin Indicates whether the package should be exported for use in Kotlin code.
             * @property branch The branch name of the remote Git repository used for the dependency.
             */
            public data class Branch(
                public override val url: String,
                public override val names: List<String>,
                public override val packageName: String = names.first(),
                override val exportToKotlin: Boolean = false,
                public val branch: String,
            ) : Remote

            /**
             * Represents a specific remote commit dependency for a Swift Package.
             *
             * @property url The URL of the remote repository that hosts the dependency.
             * @property names A list of possible names associated with the dependency.
             * @property packageName The default package name, derived from the first element of `names` by default.
             * @property exportToKotlin Indicates whether this dependency should be exported to Kotlin targets.
             * @property revision A specific commit hash representing the dependency version.
             */
            public data class Commit(
                public override val url: String,
                public override val names: List<String>,
                public override val packageName: String = names.first(),
                override val exportToKotlin: Boolean = false,
                public val revision: String,
            ) : Remote
        }
    }
}
