package io.github.frankois944.spmForKmp.definition

import io.github.frankois944.spmForKmp.definition.product.dsl.ProductPackageConfig
import io.github.frankois944.spmForKmp.definition.product.dsl.ProductPackageConfigImpl
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

    public sealed interface Binary :
        SwiftDependency,
        Serializable {
        /**
         * Indicates whether this product should be exposed and made available for usage
         * in Kotlin code. Defaults to `false`.
         */
        public val exportToKotlin: Boolean

        /**
         *  Add custom linker flag when exporting the product to kotlin
         */
        public var linkerOpts: List<String>

        /**
         *  Add custom compiler flag when exporting the product to kotlin
         */
        public var compilerOpts: List<String>

        @Suppress("MaxLineLength")
        /**
         * Represents a local binary dependency in the Kotlin Multiplatform project.
         *
         * @see <a href="https://www.avanderlee.com/swift/binary-targets-swift-package-manager/#local-binary-targets">How to make one</a>
         *
         * @property path The local file URL (file://...) to the xcFramework.
         * @property packageName The name of the package associated with this binary.
         * @property exportToKotlin Defines whether the dependency should be exported for use in Kotlin code.
         * @property linkerOpts
         * @property compilerOpts
         */
        @Deprecated("Replace with localBinary from dependency(dependency: DependencyConfig.() -> Unit)")
        public data class Local(
            val path: String,
            override val packageName: String,
            override val exportToKotlin: Boolean = false,
            override var linkerOpts: List<String> = emptyList(),
            override var compilerOpts: List<String> = emptyList(),
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
         * @property linkerOpts
         * @property compilerOpts
         */
        @Deprecated("Replace with remoteBinary from dependency(dependency: DependencyConfig.() -> Unit)")
        public data class Remote(
            val url: URI,
            override val packageName: String,
            override val exportToKotlin: Boolean = false,
            val checksum: String,
            override var linkerOpts: List<String> = emptyList(),
            override var compilerOpts: List<String> = emptyList(),
        ) : Binary
    }

    public sealed class Package :
        SwiftDependency,
        Serializable {
        public var productsConfig: ProductPackageConfig = ProductPackageConfigImpl()
            internal set
        internal abstract val products: ProductPackageConfig.() -> Unit

        /**
         * Represents a local Swift package dependency.
         *
         * @property path The local file URL (file://...) to the local Swift package folder.
         * @property packageName The name of the package, by default the first product name.
         * @property products A list of the product's package used during dependency configuration.
         */
        @Deprecated("Replace with localPackage from dependency(dependency: DependencyConfig.() -> Unit)")
        public data class Local(
            val path: String,
            override var packageName: String = "",
            override val products: ProductPackageConfig.() -> Unit,
        ) : Package() {
            init {
                productsConfig.apply(products)
                if (packageName.isEmpty()) {
                    packageName =
                        productsConfig.productPackages
                            .first()
                            .products
                            .first()
                            .name
                }
            }
        }

        public sealed class Remote :
            Package(),
            Serializable {
            public open val url: URI = URI("")

            /**
             * Represents a specific version of a remote Swift package to be used
             * as a dependency in a Kotlin Multiplatform project.
             *
             * @property url The URL of the remote Git repository where the package is hosted.
             * @property products A list of the product's package used during dependency configuration.
             * @property packageName The name of the package, by default base of the url.
             * @property version The specific version of the Swift package to be imported.
             */
            @Deprecated("Replace with remotePackageVersion from dependency(dependency: DependencyConfig.() -> Unit)")
            public data class Version(
                public override val url: URI,
                public override var packageName: String = "",
                public val version: String,
                public override val products: ProductPackageConfig.() -> Unit,
            ) : Remote() {
                init {
                    if (packageName.isEmpty()) {
                        packageName = buildPackageName(url)
                    }
                    productsConfig.apply(products)
                }
            }

            /**
             * Represents a branch-based remote Swift dependency in a Kotlin Multiplatform project.
             *
             * @property url The URL of the remote Git repository where the package is hosted.
             * @property products A list of the product's package used during dependency configuration.
             * @property packageName The name of the package, by default base of the url.
             * @property branch The branch name of the remote Git repository used for the dependency.
             */
            @Deprecated("Replace with remotePackageBranch from dependency(dependency: DependencyConfig.() -> Unit)")
            public data class Branch(
                public override val url: URI,
                public override var packageName: String = "",
                public val branch: String,
                override val products: ProductPackageConfig.() -> Unit,
            ) : Remote() {
                init {
                    if (packageName.isEmpty()) {
                        packageName = buildPackageName(url)
                    }
                    productsConfig.apply(products)
                }
            }

            /**
             * Represents a specific remote commit dependency for a Swift Package.
             *
             * @property url The URL of the remote Git repository where the package is hosted.
             * @property products A list of the product's package used during dependency configuration.
             * @property packageName The name of the package, by default base of the url.
             * @property revision A specific commit hash representing the dependency version.
             */
            @Deprecated("Replace with remotePackageCommit from dependency(dependency: DependencyConfig.() -> Unit)")
            public data class Commit(
                public override val url: URI,
                public override var packageName: String = "",
                public val revision: String,
                override val products: ProductPackageConfig.() -> Unit,
            ) : Remote() {
                init {
                    if (packageName.isEmpty()) {
                        packageName = buildPackageName(url)
                    }
                    productsConfig.apply(products)
                }
            }

            internal companion object {
                private const val serialVersionUID: Long = 2
            }
        }

        internal companion object {
            private const val serialVersionUID: Long = 2
        }
    }
}
