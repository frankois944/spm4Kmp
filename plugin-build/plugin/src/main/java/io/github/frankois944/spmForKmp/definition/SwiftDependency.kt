package io.github.frankois944.spmForKmp.definition

import java.io.Serializable
import java.net.URI

/**
 * Represents a Swift dependency within a Kotlin Multiplatform project. This sealed interface serves as the base type
 * for various Swift dependency configurations, including binary and package dependencies.
 * Dependencies can be local or remote.
 *
 * @property packageName The name of the package associated with the dependency.
 * This is utilized for identification within
 * the Swift package ecosystem.
 */
public sealed interface SwiftDependency : Serializable {
    public val packageName: String

    private companion object {
        /**
         * Extracts the package name from a given Git repository URL.
         *
         * This method takes a URI, retrieves the last path segment of the URI,
         * and removes the ".git" suffix if present. The resulting string represents
         * the package name, typically used for identifying Swift packages in dependency configurations.
         *
         * @param url The URI of the Git repository from which the package name is extracted.
         * @return A string representing the extracted package name.
         */
        private fun buildPackageName(url: URI): String =
            url.path
                .split("/")
                .last()
                .replace(".git", "")
    }

    public sealed interface Binary : SwiftDependency {
        /**
         * Indicates whether this product should be exposed and made available for usage
         * in Kotlin code. Defaults to `false`.
         */
        public val exportToKotlin: Boolean

        @Suppress("MaxLineLength")
        /**
         * Represents a local binary dependency in the Kotlin Multiplatform project.
         *
         * @see <a href="https://www.avanderlee.com/swift/binary-targets-swift-package-manager/#local-binary-targets">How to make one</a>
         *
         * @property path The local file URL (file://...) to the xcFramework.
         * @property packageName The name of the package associated with this binary.
         * @property exportToKotlin Defines whether the dependency should be exported for use in Kotlin code.
         */
        public data class Local(
            val path: String,
            override val packageName: String,
            override val exportToKotlin: Boolean = false,
        ) : Binary

        @Suppress("MaxLineLength")
        /**
         * Represents a remote binary dependency as a zipped xcFramework
         *
         * @see <a href="https://www.avanderlee.com/swift/binary-targets-swift-package-manager/#defining-a-binary-target">How to make one</a>
         *
         * @property url The URL pointing to the remote binary artifact.
         * @property packageName The name of the package associated with this binary dependency.
         * @property exportToKotlin Defines whether this dependency should be exported for use in Kotlin code.
         * @property checksum The checksum of the remote binary to verify its integrity.
         */
        public data class Remote(
            val url: URI,
            override val packageName: String,
            override val exportToKotlin: Boolean = false,
            val checksum: String,
        ) : Binary
    }

    public sealed interface Package : SwiftDependency {
        public val products: List<ProductPackageConfig>

        /**
         * Represents a local Swift package dependency.
         *
         * @property path The local file URL (file://...) to the local Swift package folder.
         * @property packageName The name of the package.
         * @property products A list of the product's package used during dependency configuration.
         */
        public data class Local(
            val path: String,
            override val packageName: String = products.first().name,
            override val products: List<ProductPackageConfig>,
        ) : Package

        public sealed interface Remote : Package {
            public val url: URI

            /**
             * Represents a specific version of a remote Swift package to be used
             * as a dependency in a Kotlin Multiplatform project.
             *
             * @property url The URL of the remote Git repository where the package is hosted.
             * @property products A list of the product's package used during dependency configuration.
             * @property packageName The name of the package.
             * @property version The specific version of the Swift package to be imported.
             */
            public data class Version(
                public override val url: URI,
                public override val products: List<ProductPackageConfig>,
                public override val packageName: String = buildPackageName(url),
                public val version: String,
            ) : Remote

            /**
             * Represents a branch-based remote Swift dependency in a Kotlin Multiplatform project.
             *
             * @property url The URL of the remote Git repository where the package is hosted.
             * @property products A list of the product's package used during dependency configuration.
             * @property packageName The name of the package.
             * @property branch The branch name of the remote Git repository used for the dependency.
             */
            public data class Branch(
                public override val url: URI,
                public override val products: List<ProductPackageConfig>,
                public override val packageName: String = buildPackageName(url),
                public val branch: String,
            ) : Remote

            /**
             * Represents a specific remote commit dependency for a Swift Package.
             *
             * @property url The URL of the remote Git repository where the package is hosted.
             * @property products A list of the product's package used during dependency configuration.
             * @property packageName The name of the package.
             * @property revision A specific commit hash representing the dependency version.
             */
            public data class Commit(
                public override val url: URI,
                public override val products: List<ProductPackageConfig>,
                public override val packageName: String = buildPackageName(url),
                public val revision: String,
            ) : Remote
        }
    }
}
