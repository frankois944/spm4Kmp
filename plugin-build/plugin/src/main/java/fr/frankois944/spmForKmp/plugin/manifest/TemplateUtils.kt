package fr.frankois944.spmForKmp.plugin.manifest

import fr.frankois944.spmForKmp.plugin.definition.SwiftDependency

internal val SwiftDependency.isBinaryDependency: Boolean
    get() =
        (this is SwiftDependency.Binary.Local) ||
            (this is SwiftDependency.Binary.Remote)

internal fun SwiftDependency.toDependencyDeclaration(): String? =
    when (this) {
        is SwiftDependency.Package.Local ->
            """
            .package(path: "$path")
            """.trimIndent()

        is SwiftDependency.Package.Remote.Version ->
            """
            .package(url: "$url", exact: "$version")
            """.trimIndent()

        is SwiftDependency.Package.Remote.Commit -> {
            """
            .package(url: "$url", revision: "$revision")
            """.trimIndent()
        }

        is SwiftDependency.Package.Remote.Branch ->
            """
            .package(url: "$url", branch: "$branch")
            """.trimIndent()

        is SwiftDependency.Binary.Local -> null
        is SwiftDependency.Binary.Remote -> null
    }
