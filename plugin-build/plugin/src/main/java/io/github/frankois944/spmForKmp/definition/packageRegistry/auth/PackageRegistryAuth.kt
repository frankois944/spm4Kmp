package io.github.frankois944.spmForKmp.definition.packageRegistry.auth

import java.io.File
import java.io.Serializable
import java.net.URI

internal data class PackageRegistryAuth(
    val url: URI,
    val username: String? = null,
    val token: String? = null,
    val password: String? = null,
    val tokenFile: File? = null,
) : Serializable
