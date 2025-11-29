package io.github.frankois944.spmForKmp.definition.packageRegistry

import io.github.frankois944.spmForKmp.definition.packageRegistry.auth.PackageRegistryAuthConfig
import java.net.URI

internal data class PackageRegistry(
    override var url: URI,
    override var auth: PackageRegistryAuthConfig? = null,
) : PackageRegistryConfig
