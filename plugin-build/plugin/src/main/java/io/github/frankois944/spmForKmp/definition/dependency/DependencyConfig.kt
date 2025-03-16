package io.github.frankois944.spmForKmp.definition.dependency

import io.github.frankois944.spmForKmp.definition.product.dsl.ProductPackageConfig
import java.io.Serializable
import java.net.URI

public interface DependencyConfig : Serializable {
    public fun localBinary(
        path: String,
        packageName: String,
        exportToKotlin: Boolean = false,
        linkerOpts: List<String> = emptyList(),
        compilerOpts: List<String> = emptyList(),
    )

    @Suppress("LongParameterList")
    public fun remoteBinary(
        url: URI,
        packageName: String,
        exportToKotlin: Boolean = false,
        checksum: String,
        linkerOpts: List<String> = emptyList(),
        compilerOpts: List<String> = emptyList(),
    )

    public fun localPackage(
        path: String,
        packageName: String = "",
        products: ProductPackageConfig.() -> Unit,
    )

    public fun remotePackageVersion(
        url: URI,
        packageName: String = "",
        version: String,
        products: ProductPackageConfig.() -> Unit,
    )

    public fun remotePackageBranch(
        url: URI,
        packageName: String = "",
        branch: String,
        products: ProductPackageConfig.() -> Unit,
    )

    public fun remotePackageCommit(
        url: URI,
        packageName: String = "",
        revision: String,
        products: ProductPackageConfig.() -> Unit,
    )
}
