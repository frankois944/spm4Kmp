package io.github.frankois944.spmForKmp.definition.packageRegistry.auth

import java.io.Serializable

public interface PackageRegistryAuthConfig : Serializable {
    public var username: String?
    public var password: String?
    public var token: String?
    public var tokenFile: String?
}
