package io.github.frankois944.spmForKmp.definition.packageRegistry.auth

internal class PackageRegistryAuth(
    override var username: String? = null,
    override var password: String? = null,
    override var token: String? = null,
    override var tokenFile: String? = null,
) : PackageRegistryAuthConfig

public fun registryCredential(
    username: String,
    password: String,
): PackageRegistryAuthConfig = PackageRegistryAuth(username = username, password = password)

public fun registryToken(value: String): PackageRegistryAuthConfig = PackageRegistryAuth(token = value)

public fun registryTokenFile(path: String): PackageRegistryAuthConfig = PackageRegistryAuth(tokenFile = path)
