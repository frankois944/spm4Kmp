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
internal sealed interface SwiftDependency : Serializable {
    val packageName: String

    private companion object {
        private fun buildPackageName(url: URI): String =
            url.path
                .split("/")
                .last()
                .replace(".git", "")
    }

    sealed interface Binary :
        SwiftDependency,
        Serializable {
        /**
         * Indicates whether this product should be exposed and made available for usage
         * in Kotlin code. Defaults to `false`.
         */
        val exportToKotlin: Boolean

        val isCLang: Boolean

        data class Local(
            val path: String,
            override val packageName: String,
            override val exportToKotlin: Boolean = false,
            override val isCLang: Boolean = false,
        ) : Binary

        data class Remote(
            val url: URI,
            override val packageName: String,
            override val exportToKotlin: Boolean = false,
            val checksum: String,
            override val isCLang: Boolean = false,
        ) : Binary
    }

    sealed class Package :
        SwiftDependency,
        Serializable {
        var productsConfig: ProductPackageConfig = ProductPackageConfigImpl()
            internal set
        internal abstract val products: ProductPackageConfig.() -> Unit

        data class Local(
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

        sealed class Remote :
            Package(),
            Serializable {
            open val url: URI = URI("")

            data class Version(
                override val url: URI,
                override var packageName: String = "",
                val version: String,
                override val products: ProductPackageConfig.() -> Unit,
            ) : Remote() {
                init {
                    if (packageName.isEmpty()) {
                        packageName = buildPackageName(url)
                    }
                    productsConfig.apply(products)
                }
            }

            data class Branch(
                override val url: URI,
                override var packageName: String = "",
                val branch: String,
                override val products: ProductPackageConfig.() -> Unit,
            ) : Remote() {
                init {
                    if (packageName.isEmpty()) {
                        packageName = buildPackageName(url)
                    }
                    productsConfig.apply(products)
                }
            }

            data class Commit(
                override val url: URI,
                override var packageName: String = "",
                val revision: String,
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
                private const val serialVersionUID: Long = 4
            }
        }

        internal companion object {
            private const val serialVersionUID: Long = 4
        }
    }
}
