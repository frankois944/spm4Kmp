package io.github.frankois944.spmForKmp

import io.github.frankois944.spmForKmp.definition.PackageRootDefinitionExtension
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

private const val CONTAINER_NAME = PLUGIN_NAME // "swiftPackageConfig"

@Suppress("UNCHECKED_CAST")
internal fun Project.swiftContainer(): NamedDomainObjectContainer<PackageRootDefinitionExtension> =
    @Suppress("UNCHECKED_CAST")
    extensions.getByName(CONTAINER_NAME) as NamedDomainObjectContainer<PackageRootDefinitionExtension>

public fun KotlinNativeTarget.swiftPackage(configure: PackageRootDefinitionExtension.() -> Unit) {
    val entry = project.swiftContainer().maybeCreate(this.name)
    entry.useExtension = true
    entry.targetName = this.name
    entry.configure()
}

public fun KotlinNativeTarget.swiftPackage(
    name: String,
    configure: PackageRootDefinitionExtension.() -> Unit,
) {
    val entry = project.swiftContainer().maybeCreate(name)
    entry.useExtension = true
    entry.targetName = this.name
    entry.configure()
}
