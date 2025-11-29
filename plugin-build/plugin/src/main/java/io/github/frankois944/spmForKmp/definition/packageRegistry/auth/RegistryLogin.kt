package io.github.frankois944.spmForKmp.definition.packageRegistry.auth

import java.io.Serializable

public sealed class RegistryLogin : Serializable {
    public class Credential(
        username: String,
        password: String,
    ) : PackageRegistryAuth(username = username, password = password)

    public class Token(
        value: String,
    ) : PackageRegistryAuth(token = value)

    public class File(
        path: String,
    ) : PackageRegistryAuth(tokenFile = path)
}
