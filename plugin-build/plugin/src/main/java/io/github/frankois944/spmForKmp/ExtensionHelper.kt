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

// Per-target DSL: kotlin { iosArm64 { spm { ... } } }
public fun KotlinNativeTarget.swiftPackage(configure: PackageRootDefinitionExtension.() -> Unit) {
    val entry = project.swiftContainer().maybeCreate(this.name)
    entry.useExtension = true
    entry.configure()
}
