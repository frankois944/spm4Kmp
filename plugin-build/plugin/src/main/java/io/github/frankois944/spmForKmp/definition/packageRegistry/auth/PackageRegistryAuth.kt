package io.github.frankois944.spmForKmp.definition.packageRegistry.auth

public open class PackageRegistryAuth(
    public var username: String? = null,
    public var token: String? = null,
    public var password: String? = null,
    public var tokenFile: String? = null,
) : RegistryLogin()
