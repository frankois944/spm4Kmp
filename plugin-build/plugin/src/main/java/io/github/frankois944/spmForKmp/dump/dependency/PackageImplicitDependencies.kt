package io.github.frankois944.spmForKmp.dump.dependency

import com.fasterxml.jackson.annotation.JsonProperty
import org.gradle.internal.impldep.com.google.errorprone.annotations.Keep
import java.io.File

@Keep
internal data class PackageImplicitDependencies(
    @JsonProperty("dependencies")
    val dependencies: List<PackageImplicitDependencies>?,
    @JsonProperty("identity")
    val identity: String?,
    @JsonProperty("name")
    val name: String?,
    @JsonProperty("path")
    val path: String?,
    @JsonProperty("url")
    val url: String?,
    @JsonProperty("version")
    val version: String?,
) {
    companion object {
        private val objectMapper =
            com.fasterxml.jackson.databind
                .ObjectMapper()

        fun fromString(input: String): PackageImplicitDependencies =
            objectMapper.readValue(
                input,
                PackageImplicitDependencies::class.java,
            )
    }

    fun getFolders(vararg names: String = arrayOf("Public")): Set<File> =
        buildSet {
            dependencies?.forEach { dependency ->
                dependency.dependencies?.forEach { subDep ->
                    subDep.path?.let { path ->
                        addAll(walkFolder(path, *names))
                    }
                    addAll(subDep.getFolders(names = names))
                }
            }
        }

    private fun walkFolder(
        path: String,
        vararg names: String,
    ): Set<File> =
        try {
            File(path)
                .walk()
                .filter { file ->
                    file.isDirectory && names.contains(file.name)
                }.toSet()
        } catch (e: Exception) {
            emptySet()
        }
}
