package fr.frankois944.spm.kmp.plugin.definition

import java.io.Serializable

public sealed interface SwiftPackageDependencyDefinition : Serializable {
    public val packageName: String

    public val names: List<String>

    public val export: Boolean

    /**
     * Local swift package `directory`
     *
     * @property path The absolute path to the package directory
     * @property packageName Name of the Swift package
     * @property names The names of the products of the package
     * @property export Make the package available from kotlin, Only @objc compatible content are exported
     * @constructor
     */
    public data class Local(
        val path: String,
        override val packageName: String,
        override val names: List<String> = listOf(packageName),
        override val export: Boolean = true,
    ) : SwiftPackageDependencyDefinition

    /**
     * Local `xcframework` bundle
     *
     * @property path The absolute path to the `xcframework` bundle
     * @property packageName Name of the Swift package
     * @property names The names of the products of the package
     * @property export Make the package available from kotlin, Only @objc compatible content are exported
     * @constructor
     */
    public data class LocalBinary(
        // must be an absolute path
        val path: String,
        override val packageName: String,
        override val names: List<String> = listOf(packageName),
        override val export: Boolean = true,
    ) : SwiftPackageDependencyDefinition

    /**
     * Remove `xcframework` bundle, it must be compressed with .zip
     *
     * @property url An url to the zipped `xcframework`
     * @property packageName Name of the Swift package
     * @property names The names of the products of the package
     * @property export Make the package available from kotlin, Only @objc compatible content are exported
     * @property checksum The checksum of the `xcframework` bundle
     * @constructor
     */
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
