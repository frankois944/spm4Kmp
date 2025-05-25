package io.github.frankois944.spmForKmp.definition.dependency

import io.github.frankois944.spmForKmp.definition.product.dsl.ProductPackageConfig
import java.io.Serializable
import java.net.URI

public interface DependencyConfig : Serializable {
    @Suppress("MaxLineLength", "LongParameterList")
    /**
     * Represents a local binary dependency.
     *
     * @see <a href="https://www.avanderlee.com/swift/binary-targets-swift-package-manager/#local-binary-targets">How to make one</a>
     *
     * @property path The local file URL (file://...) to the xcFramework.
     * @property packageName The name of the package associated with this binary.
     * @property exportToKotlin Defines whether the dependency should be exported for use in Kotlin code.
     * @property isIncludedInExportedPackage if false, the whole package will be skipped inside the local package.
     * @property linkerOpts
     * @property compilerOpts
     */
    public fun localBinary(
        path: String,
        packageName: String,
        exportToKotlin: Boolean = false,
        isIncludedInExportedPackage: Boolean = true,
        linkerOpts: List<String> = emptyList(),
        compilerOpts: List<String> = emptyList(),
    )

    @Suppress("LongParameterList", "MaxLineLength")
    /**
     * Represents a remote binary dependency as a zipped xcFramework
     *
     * @see <a href="https://www.avanderlee.com/swift/binary-targets-swift-package-manager/#defining-a-binary-target">How to make one</a>
     *
     * @property url The URL pointing to the remote binary artifact.
     * @property packageName The name of the package associated with this binary dependency.
     * @property exportToKotlin Defines whether this dependency should be exported for use in Kotlin code.
     * @property checksum The checksum of the remote binary to verify its integrity.
     * @property isIncludedInExportedPackage if false, the whole package will be skipped inside the local package.
     * @property linkerOpts
     * @property compilerOpts
     */
    public fun remoteBinary(
        url: URI,
        packageName: String,
        exportToKotlin: Boolean = false,
        checksum: String,
        isIncludedInExportedPackage: Boolean = true,
        linkerOpts: List<String> = emptyList(),
        compilerOpts: List<String> = emptyList(),
    )

    /**
     * Represents a local Swift package dependency.
     *
     * @property path The local file URL (file://...) to the local Swift package folder.
     * @property packageName The name of the package, by default the first product name.
     * @property isIncludedInExportedPackage if false, the whole package will be skipped inside the local package.
     * @property products A list of the product's package used during dependency configuration.
     */
    public fun localPackage(
        path: String,
        packageName: String = "",
        isIncludedInExportedPackage: Boolean = true,
        products: ProductPackageConfig.() -> Unit,
    )

    /**
     * Represents a specific version of a remote Swift package.
     *
     * @property url The URL of the remote Git repository where the package is hosted.
     * @property packageName The name of the package, by default base of the url.
     * @property version The specific version of the Swift package to be imported.
     * @property isIncludedInExportedPackage if false, the whole package will be skipped inside the local package.
     * @property products A list of the product's package used during dependency configuration.
     */
    public fun remotePackageVersion(
        url: URI,
        packageName: String = "",
        version: String,
        isIncludedInExportedPackage: Boolean = true,
        products: ProductPackageConfig.() -> Unit,
    )

    /**
     * Represents a branch-based remote Swift dependency.
     *
     * @property url The URL of the remote Git repository where the package is hosted.
     * @property packageName The name of the package, by default base of the url.
     * @property branch The branch name of the remote Git repository used for the dependency.
     * @property isIncludedInExportedPackage if false, the whole package will be skipped inside the local package.
     * @property products A list of the product's package used during dependency configuration.
     *
     */
    public fun remotePackageBranch(
        url: URI,
        packageName: String = "",
        branch: String,
        isIncludedInExportedPackage: Boolean = true,
        products: ProductPackageConfig.() -> Unit,
    )

    /**
     * Represents a specific remote commit dependency.
     *
     * @property url The URL of the remote Git repository where the package is hosted.
     * @property packageName The name of the package, by default base of the url.
     * @property revision A specific commit hash representing the dependency version.
     * @property isIncludedInExportedPackage if false, the whole package will be skipped inside the local package.
     * @property products A list of the product's package used during dependency configuration.
     *
     */
    public fun remotePackageCommit(
        url: URI,
        packageName: String = "",
        revision: String,
        isIncludedInExportedPackage: Boolean = true,
        products: ProductPackageConfig.() -> Unit,
    )
}
