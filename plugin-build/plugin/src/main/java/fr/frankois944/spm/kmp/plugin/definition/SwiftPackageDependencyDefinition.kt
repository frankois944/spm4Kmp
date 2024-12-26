package fr.frankois944.spm.kmp.plugin.definition

import java.io.Serializable

public sealed interface SwiftPackageDependencyDefinition : Serializable {
    public val names: List<String>
    public val packageName: String

    public data class Local(
        val path: String,
        override val names: List<String>,
        override val packageName: String = names.first(),
    ) : SwiftPackageDependencyDefinition

    public data class LocalBinary(
        val path: String,
        override val names: List<String>,
        override val packageName: String = names.first(),
    ) : SwiftPackageDependencyDefinition

    public data class RemoteBinary(
        val url: String,
        override val names: List<String>,
        override val packageName: String = names.first(),
        val checksum: String,
    ) : SwiftPackageDependencyDefinition

    public sealed interface RemoteDefinition : SwiftPackageDependencyDefinition {
        public val url: String

        public data class Version(
            public override val url: String,
            public override val names: List<String>,
            public override val packageName: String = names.first(),
            public val version: String,
        ) : RemoteDefinition

        public data class Branch(
            public override val url: String,
            public override val names: List<String>,
            public override val packageName: String = names.first(),
            public val branch: String,
        ) : RemoteDefinition

        public data class Commit(
            public override val url: String,
            public override val names: List<String>,
            public override val packageName: String = names.first(),
            public val revision: String,
        ) : RemoteDefinition
    }
}
