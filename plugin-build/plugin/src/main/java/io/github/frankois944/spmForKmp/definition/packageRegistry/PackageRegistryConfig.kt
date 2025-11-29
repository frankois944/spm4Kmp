package io.github.frankois944.spmForKmp.definition.packageRegistry

import io.github.frankois944.spmForKmp.definition.packageRegistry.auth.RegistryLogin
import java.io.Serializable
import java.net.URI

public interface PackageRegistryConfig : Serializable {
    public var url: URI
    public var auth: RegistryLogin?
}
