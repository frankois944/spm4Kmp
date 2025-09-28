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
        isCLang: Boolean,
    ) {
        packageDependencies.add(
            SwiftDependency.Binary.Local(
                path = path,
                packageName = packageName,
                exportToKotlin = exportToKotlin,
                isCLang = isCLang,
            ),
        )
    }

    override fun remoteBinary(
        url: URI,
        packageName: String,
        exportToKotlin: Boolean,
        checksum: String,
        isCLang: Boolean,
    ) {
        packageDependencies.add(
            SwiftDependency.Binary.Remote(
                url = url,
                packageName = packageName,
                exportToKotlin = exportToKotlin,
                checksum = checksum,
                isCLang = isCLang,
            ),
        )
    }

    override fun localPackage(
        path: String,
        packageName: String,
        products: ProductPackageConfig.() -> Unit,
    ) {
        packageDependencies.add(
            SwiftDependency.Package.Local(
                path = path,
                packageName = packageName,
                products = products,
            ),
        )
    }

    override fun remotePackageVersion(
        url: URI,
        packageName: String,
        version: String,
        products: ProductPackageConfig.() -> Unit,
    ) {
        packageDependencies.add(
            SwiftDependency.Package.Remote.Version(
                url = url,
                packageName = packageName,
                version = version,
                products = products,
            ),
        )
    }

    override fun remotePackageBranch(
        url: URI,
        packageName: String,
        branch: String,
        products: ProductPackageConfig.() -> Unit,
    ) {
        packageDependencies.add(
            SwiftDependency.Package.Remote.Branch(
                url = url,
                packageName = packageName,
                branch = branch,
                products = products,
            ),
        )
    }

    override fun remotePackageCommit(
        url: URI,
        packageName: String,
        revision: String,
        products: ProductPackageConfig.() -> Unit,
    ) {
        packageDependencies.add(
            SwiftDependency.Package.Remote.Commit(
                url = url,
                packageName = packageName,
                revision = revision,
                products = products,
            ),
        )
    }

    private companion object {
        private const val serialVersionUID: Long = 2
    }
}
