package io.github.frankois944.spmForKmp.dump

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import org.gradle.internal.cc.base.logger
import org.gradle.internal.impldep.com.google.errorprone.annotations.Keep
import java.io.File

@Keep
internal data class PackageImplicitDependencies(
    @param:JsonProperty("dependencies")
    val dependencies: List<PackageImplicitDependencies>?,
    @param:JsonProperty("identity")
    val identity: String?,
    @param:JsonProperty("name")
    val name: String?,
    @param:JsonProperty("path")
    val path: String?,
    @param:JsonProperty("url")
    val url: String?,
    @param:JsonProperty("version")
    val version: String?,
) {
    companion object {
        private val objectMapper =
            ObjectMapper()

        fun fromString(input: String): PackageImplicitDependencies =
            objectMapper.readValue(
                input,
                PackageImplicitDependencies::class.java,
            )
    }

    fun getPublicFolders(): Set<File> =
        searchInFolder(
            dependencies = this.dependencies,
        ).distinct()
            .toSet()

    private fun searchInFolder(dependencies: List<PackageImplicitDependencies>?): Set<File> {
        val names = arrayOf("Public")
        val results = mutableSetOf<File>()
        dependencies?.forEach { dependency ->
            dependency.dependencies?.forEach { subDep ->
                subDep.path?.let { path ->
                    val found = walkFolder(path, names = names)
                    results.addAll(found)
                }
                results.addAll(searchInFolder(subDep.dependencies))
            }
        }
        return results
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
        } catch (ex: Exception) {
            logger.error("Cant look for dependencies in path: $path", ex)
            emptySet()
        }
}
