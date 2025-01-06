package io.github.frankois944.spmForKmp.dump.manifest

import com.fasterxml.jackson.annotation.JsonProperty
import org.gradle.internal.impldep.com.google.errorprone.annotations.Keep

@Keep
internal data class PackageDumpX(
    @JsonProperty("cLanguageStandard")
    val cLanguageStandard: String?,
    @JsonProperty("cxxLanguageStandard")
    val cxxLanguageStandard: String?,
    @JsonProperty("dependencies")
    val dependencies: List<Dependency>?,
    @JsonProperty("name")
    val name: String?,
    @JsonProperty("packageKind")
    val packageKind: PackageKind?,
    @JsonProperty("pkgConfig")
    val pkgConfig: Any?,
    @JsonProperty("platforms")
    val platforms: List<Platform>?,
    @JsonProperty("products")
    val products: List<Product>?,
    @JsonProperty("providers")
    val providers: Any?,
    @JsonProperty("swiftLanguageVersions")
    val swiftLanguageVersions: Any?,
    @JsonProperty("targets")
    val targets: List<Target>?,
    @JsonProperty("toolsVersion")
    val toolsVersion: ToolsVersion?,
)
