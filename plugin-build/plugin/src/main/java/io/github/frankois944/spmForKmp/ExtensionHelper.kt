package io.github.frankois944.spmForKmp

import io.github.frankois944.spmForKmp.definition.PackageRootDefinitionExtension
import io.github.frankois944.spmForKmp.utils.ExperimentalSpmForKmpFeature
import org.gradle.api.GradleException
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

private const val CONTAINER_NAME = PLUGIN_NAME // "swiftPackageConfig"

@Suppress("UNCHECKED_CAST")
internal fun Project.swiftContainer(): NamedDomainObjectContainer<PackageRootDefinitionExtension> =
    extensions.getByName(CONTAINER_NAME) as NamedDomainObjectContainer<PackageRootDefinitionExtension>

/**
 * Configures a Swift package for the given Kotlin Native target. This allows seamless integration
 * of Swift and Kotlin code in a multiplatform project by enabling the use of a Swift package.
 *
 * @param configure A lambda function to customize the package configuration
 *                  using the provided `PackageRootDefinitionExtension`.
 */
@ExperimentalSpmForKmpFeature
public fun KotlinNativeTarget.swiftPackageConfig(configure: PackageRootDefinitionExtension.() -> Unit) {
    val entry = project.swiftContainer().maybeCreate(this.name)
    entry.useExtension = true
    entry.targetName = this.name
    entry.configure()
}

/**
 * Configures a Swift Package for the specified Kotlin Native target.
 *
 * @param cinteropName The name of the group to associate with a group of targets; useful when using a list of target
 * @param configure A lambda used to configure the package using the provided `PackageRootDefinitionExtension`.
 */
@ExperimentalSpmForKmpFeature
public fun KotlinNativeTarget.swiftPackageConfig(
    cinteropName: String,
    configure: PackageRootDefinitionExtension.() -> Unit,
) {
    if (cinteropName.isEmpty()) {
        throw GradleException("The cinteropName cannot be empty")
    }
    val entry = project.swiftContainer().maybeCreate(cinteropName)
    entry.useExtension = true
    entry.targetName = this.name
    entry.configure()
}
