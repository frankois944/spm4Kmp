package fr.frankois944.spm.kmp.plugin.definition

import java.io.Serializable

public sealed interface SwiftPackageDependencyDefinition : Serializable {
    public val names: List<String>
    public val packageName: String
    public val export: Boolean

    public data class Local(
        val path: String,
        override val names: List<String>,
        override val packageName: String = names.first(),
        override val export: Boolean = true,
    ) : SwiftPackageDependencyDefinition

    public data class LocalBinary(
        // must be an absolute path from the system
        val path: String,
        override val packageName: String,
        override val names: List<String> = listOf(packageName),
        override val export: Boolean = true,
    ) : SwiftPackageDependencyDefinition

    public data class RemoteBinary(
        val url: String,
        override val packageName: String,
        override val names: List<String> = listOf(packageName),
        override val export: Boolean = true,
        val checksum: String,
    ) : SwiftPackageDependencyDefinition

    public sealed interface RemoteDefinition : SwiftPackageDependencyDefinition {
        public val url: String

        public data class Version(
            public override val url: String,
            public override val names: List<String>,
            public override val packageName: String = names.first(),
            override val export: Boolean = true,
            public val version: String,
        ) : RemoteDefinition

        public data class Branch(
            public override val url: String,
            public override val names: List<String>,
            public override val packageName: String = names.first(),
            override val export: Boolean = true,
            public val branch: String,
        ) : RemoteDefinition

        public data class Commit(
            public override val url: String,
            public override val names: List<String>,
            public override val packageName: String = names.first(),
            override val export: Boolean = true,
            public val revision: String,
        ) : RemoteDefinition
    }
}
