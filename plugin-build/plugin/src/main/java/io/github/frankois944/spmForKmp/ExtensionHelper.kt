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

/**
 * Configures a Swift package for the given Kotlin Native target. This allows seamless integration
 * of Swift and Kotlin code in a multiplatform project by enabling the use of a Swift package.
 *
 * @param configure A lambda function to customize the package configuration
 *                  using the provided `PackageRootDefinitionExtension`.
 */
public fun KotlinNativeTarget.swiftPackage(configure: PackageRootDefinitionExtension.() -> Unit) {
    val entry = project.swiftContainer().maybeCreate(this.name)
    entry.useExtension = true
    entry.targetName = this.name
    entry.configure()
}

/**
 * Configures a Swift Package for the specified Kotlin Native target.
 *
 * @param groupName The name of the group to associate with a group Of Target; useful when using a list of target
 * @param configure A lambda used to configure the package using the provided `PackageRootDefinitionExtension`.
 */
public fun KotlinNativeTarget.swiftPackage(
    groupName: String,
    configure: PackageRootDefinitionExtension.() -> Unit,
) {
    val entry = project.swiftContainer().maybeCreate(groupName)
    entry.useExtension = true
    entry.targetName = this.name
    entry.configure()
}
