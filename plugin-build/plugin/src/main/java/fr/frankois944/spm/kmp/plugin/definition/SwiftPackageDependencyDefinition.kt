package fr.frankois944.spm.kmp.plugin.definition

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import java.io.File
import java.net.URI
import java.net.URL

public sealed interface SwiftPackageDependencyDefinition {
    @get:Input
    public val names: List<String>

    @get:Input
    @get:Optional
    public val packageName: String

    public data class Local(
        @InputDirectory
        val path: File,
        @Input
        override val names: List<String>,
        @Input
        @get:Optional
        override val packageName: String = names.first(),
    ) : SwiftPackageDependencyDefinition

    public data class LocalBinary(
        @InputDirectory
        val path: File,
        @Input
        override val names: List<String>,
        @Input
        @get:Optional
        override val packageName: String = names.first(),
    ) : SwiftPackageDependencyDefinition

    public data class RemoteBinary(
        @get:Input
        val url: URL,
        @get:Input
        override val names: List<String>,
        @get:Input
        @get:Optional
        override val packageName: String = names.first(),
        @get:Input
        val checksum: String,
    ) : SwiftPackageDependencyDefinition

    public sealed interface RemoteDefinition : SwiftPackageDependencyDefinition {
        @get:Input
        public val url: URI

        public data class Version(
            @get:Input
            public override val url: URI,
            @get:Input
            public override val names: List<String>,
            @get:Input
            @get:Optional
            public override val packageName: String = names.first(),
            @get:Input
            public val version: String,
        ) : RemoteDefinition

        public data class Branch(
            @get:Input
            public override val url: URI,
            @get:Input
            public override val names: List<String>,
            @get:Input
            @get:Optional
            public override val packageName: String = names.first(),
            @get:Input
            public val branch: String,
        ) : RemoteDefinition

        public data class Commit(
            @get:Input
            public override val url: URI,
            @get:Input
            public override val names: List<String>,
            @get:Input
            @get:Optional
            public override val packageName: String = names.first(),
            @get:Input
            public val revision: String,
        ) : RemoteDefinition
    }
}
