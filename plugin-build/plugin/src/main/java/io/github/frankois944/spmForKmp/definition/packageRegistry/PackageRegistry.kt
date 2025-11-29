package io.github.frankois944.spmForKmp.definition.packageRegistry

import io.github.frankois944.spmForKmp.definition.packageRegistry.auth.RegistryLogin
import java.net.URI

internal data class PackageRegistry(
    override var url: URI,
    override var auth: RegistryLogin? = null,
) : PackageRegistryConfig
