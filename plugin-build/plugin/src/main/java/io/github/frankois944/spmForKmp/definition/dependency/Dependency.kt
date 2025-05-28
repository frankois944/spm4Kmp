package io.github.frankois944.spmForKmp.definition.dependency

import io.github.frankois944.spmForKmp.definition.SwiftDependency
import io.github.frankois944.spmForKmp.definition.product.dsl.ProductPackageConfig
import java.io.Serializable
import java.net.URI

internal class Dependency :
    DependencyConfig,
    Serializable {
    internal val packageDependencies: MutableList<SwiftDependency> = mutableListOf()

    override fun localBinary(
        path: String,
        packageName: String,
        exportToKotlin: Boolean,
        isIncludedInExportedPackage: Boolean,
        linkerOpts: List<String>,
        compilerOpts: List<String>,
    ) {
        packageDependencies.add(
            SwiftDependency.Binary.Local(
                path = path,
                packageName = packageName,
                exportToKotlin = exportToKotlin,
                linkerOpts = linkerOpts,
                compilerOpts = compilerOpts,
                isIncludedInExportedPackage = isIncludedInExportedPackage,
            ),
        )
    }

    override fun remoteBinary(
        url: URI,
        packageName: String,
        exportToKotlin: Boolean,
        checksum: String,
        isIncludedInExportedPackage: Boolean,
        linkerOpts: List<String>,
        compilerOpts: List<String>,
    ) {
        packageDependencies.add(
            SwiftDependency.Binary.Remote(
                url = url,
                packageName = packageName,
                exportToKotlin = exportToKotlin,
                checksum = checksum,
                linkerOpts = linkerOpts,
                compilerOpts = compilerOpts,
                isIncludedInExportedPackage = isIncludedInExportedPackage,
            ),
        )
    }

    override fun localPackage(
        path: String,
        packageName: String,
        isIncludedInExportedPackage: Boolean,
        products: ProductPackageConfig.() -> Unit,
    ) {
        packageDependencies.add(
            SwiftDependency.Package.Local(
                path = path,
                packageName = packageName,
                products = products,
                isIncludedInExportedPackage = isIncludedInExportedPackage,
            ),
        )
    }

    override fun remotePackageVersion(
        url: URI,
        packageName: String,
        version: String,
        isIncludedInExportedPackage: Boolean,
        products: ProductPackageConfig.() -> Unit,
    ) {
        packageDependencies.add(
            SwiftDependency.Package.Remote.Version(
                url = url,
                packageName = packageName,
                version = version,
                products = products,
                isIncludedInExportedPackage = isIncludedInExportedPackage,
            ),
        )
    }

    override fun remotePackageBranch(
        url: URI,
        packageName: String,
        branch: String,
        isIncludedInExportedPackage: Boolean,
        products: ProductPackageConfig.() -> Unit,
    ) {
        packageDependencies.add(
            SwiftDependency.Package.Remote.Branch(
                url = url,
                packageName = packageName,
                branch = branch,
                products = products,
                isIncludedInExportedPackage = isIncludedInExportedPackage,
            ),
        )
    }

    override fun remotePackageCommit(
        url: URI,
        packageName: String,
        revision: String,
        isIncludedInExportedPackage: Boolean,
        products: ProductPackageConfig.() -> Unit,
    ) {
        packageDependencies.add(
            SwiftDependency.Package.Remote.Commit(
                url = url,
                packageName = packageName,
                revision = revision,
                products = products,
                isIncludedInExportedPackage = isIncludedInExportedPackage,
            ),
        )
    }

    private companion object {
        private const val serialVersionUID: Long = 2
    }
}
