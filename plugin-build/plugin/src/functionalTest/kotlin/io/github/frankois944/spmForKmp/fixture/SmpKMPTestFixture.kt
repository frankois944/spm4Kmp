package io.github.frankois944.spmForKmp.fixture

import com.autonomousapps.kit.AbstractGradleProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.RootProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.Subproject
import com.autonomousapps.kit.gradle.Imports
import com.autonomousapps.kit.gradle.Plugin
import io.github.frankois944.spmForKmp.config.AppleCompileTarget
import io.github.frankois944.spmForKmp.definition.SwiftDependency
import io.github.frankois944.spmForKmp.definition.product.ProductConfig
import io.github.frankois944.spmForKmp.definition.product.ProductName
import org.gradle.internal.cc.base.logger
import org.intellij.lang.annotations.Language

abstract class SmpKMPTestFixture private constructor(
    private val configuration: TestConfiguration,
) : AbstractGradleProject(configuration.buildPath) {
    private val jacocoDestfile: String? get() = System.getProperty("jacocoDestfile")?.replace("""\""", """\\""")
    private var _gradleProject: GradleProject? = null

    val gradleProject: GradleProject
        get() = _gradleProject ?: createProject().also { _gradleProject = it }

    data class TestConfiguration(
        val buildPath: String = "build/functionalTest",
        var customPackageSourcePath: String = "src/swift",
        var cinteropsName: String = "dummy",
        var minIos: String? = "12.0",
        var minMacos: String? = "10.15",
        var minTvos: String? = "12.0",
        var minWatchos: String? = "4.0",
        var toolsVersion: String = "5.9",
        var packageDependencyPrefix: String? = null,
        var targets: List<AppleCompileTarget> = listOf(AppleCompileTarget.iosSimulatorArm64),
        val swiftSources: List<SwiftSource> = emptyList(),
        val kotlinSources: List<KotlinSource> = emptyList(),
        val packages: List<SwiftDependency> = emptyList(),
        val sharedCachePath: String? = null,
        val sharedConfigPath: String? = null,
        val sharedSecurityPath: String? = null,
        val customSPMPath: String? = null,
        val rawDependencyConfiguration: List<KotlinSource> = emptyList(),
        val rawPluginConfiguration: List<KotlinSource> = emptyList(),
        val rawPluginRootConfig: String? = null,
        var copyDependenciesToApp: Boolean = false,
    )

    protected abstract fun createProject(): GradleProject

    protected fun createDefaultProject(extension: TestConfiguration): GradleProject =
        newGradleProjectBuilder(GradleProject.DslKind.KOTLIN)
            .withRootProject {
                setupProperties()
            }.withSubproject("library") {
                setupSources(extension.cinteropsName)
                setupGradleConfig(extension)
            }.write()

    private fun RootProject.Builder.setupProperties() {
        var content = """
kotlin.mpp.enableCInteropCommonization=true
org.gradle.caching=true
"""
        // code coverage
        if (jacocoDestfile != null) {
            content +=
                """
                # code coverage
                systemProp.jacoco-agent.destfile=$jacocoDestfile
                systemProp.jacoco-agent.append=true
                systemProp.jacoco-agent.dumponexit=false
                systemProp.jacoco-agent.jmx=true
                """.trimIndent()
        }
        withFile(
            "gradle.properties",
            content,
        )
    }

    private fun Subproject.Builder.setupSources(cinteropsName: String) {
        configuration.swiftSources.forEach { source ->
            withFile(
                "src/swift/$cinteropsName/${source.filename}",
                source.content,
            )
        }

        val kotlinSources =
            configuration.kotlinSources.ifEmpty {
                listOf(KotlinSource.default())
            }
        kotlinSources.forEach { source ->
            sources.add(
                Source
                    .kotlin(
                        """
                        package ${source.packageName}
                        ${source.imports.joinToString(separator = "\n") { "import $it" }}
                        ${source.content}
                        """.trimIndent(),
                    ).withPath(source.packageName, source.className)
                    .withSourceSet("appleMain")
                    .build(),
            )
        }
    }

    private fun Subproject.Builder.setupGradleConfig(extension: TestConfiguration) {
        withBuildScript {
            imports =
                Imports.of(
                    "io.github.frankois944.spmForKmp.definition.SwiftDependency",
                    "java.net.URI",
                    "java.lang.management.ManagementFactory",
                    "javax.management.ObjectName",
                    "io.github.frankois944.spmForKmp.definition.product.ProductName",
                )
            plugins(
                Plugin(
                    "org.jetbrains.kotlin.multiplatform",
                    System.getProperty("com.autonomousapps.test.versions.kotlin"),
                ),
                Plugin(
                    "io.github.frankois944.spmForKmp",
                    System.getProperty("com.autonomousapps.plugin-under-test.version"),
                ),
            )
            withKotlin(createPluginBlock(extension))
        }
    }

    private fun createPluginBlock(extension: TestConfiguration): String {
        val pluginBlock =
            if (extension.rawPluginConfiguration.isNotEmpty()) {
                buildString {
                    appendLine(
                        """
                        swiftPackageConfig {
                            create("${extension.cinteropsName}") {
                        """,
                    )
                    extension.rawPluginConfiguration.forEach {
                        appendLine(it.content)
                    }
                    appendLine(
                        """
                            }
                        }
                        """.trimIndent(),
                    )
                }
            } else {
                buildString {
                    append(
                        """
swiftPackageConfig {
    create("${extension.cinteropsName}") {
    customPackageSourcePath = "${extension.customPackageSourcePath}"
    toolsVersion = "${extension.toolsVersion}"
    copyDependenciesToApp = ${extension.copyDependenciesToApp}
""",
                    )
                    extension.minIos?.let {
                        appendLine("minIos = \"${extension.minIos}\"")
                    } ?: run {
                        appendLine("minIos = null")
                    }

                    extension.minMacos?.let {
                        appendLine("minMacos = \"${extension.minMacos}\"")
                    } ?: run {
                        appendLine("minMacos = null")
                    }

                    extension.minTvos?.let {
                        appendLine("minTvos = \"${extension.minTvos}\"")
                    } ?: run {
                        appendLine("minTvos = null")
                    }

                    extension.minWatchos?.let {
                        appendLine("minWatchos = \"${extension.minWatchos}\"")
                    } ?: run {
                        appendLine("minWatchos = null")
                    }
                    extension.packageDependencyPrefix?.let {
                        appendLine("packageDependencyPrefix = \"${extension.packageDependencyPrefix}\"")
                    }
                    extension.sharedCachePath?.let {
                        appendLine("sharedCachePath = \"${extension.sharedCachePath}\"")
                    }
                    extension.sharedConfigPath?.let {
                        appendLine("sharedConfigPath = \"${extension.sharedConfigPath}\"")
                    }
                    extension.sharedSecurityPath?.let {
                        appendLine("sharedSecurityPath = \"${extension.sharedSecurityPath}\"")
                    }
                    extension.customSPMPath?.let {
                        appendLine("spmWorkingPath = \"${extension.customSPMPath}\"")
                    }

                    fun buildProductBlock(
                        name: ProductName,
                        isLast: Boolean,
                    ): String =
                        buildString {
                            append("ProductName(")
                            append("name = \"${name.name}\"")
                            name.alias?.let { alias ->
                                append(", alias = \"$alias\"")
                            }
                            if (!name.isIncludedInExportedPackage) {
                                append(", isIncludedInExportedPackage = false")
                            }
                            appendLine(")")
                            if (!isLast) {
                                appendLine(",")
                            }
                        }

                    fun buildPackageBlock(config: ProductConfig): String =
                        buildString {
                            appendLine("add(")
                            config.products.forEachIndexed { index, name ->
                                append(
                                    buildProductBlock(
                                        name = name,
                                        isLast = name == config.products.last(),
                                    ),
                                )
                            }
                            if (config.exportToKotlin) {
                                appendLine(", exportToKotlin = ${config.exportToKotlin}")
                            }
                            appendLine(")")
                        }

                    extension.rawPluginRootConfig?.let {
                        appendLine(it)
                    }
                    appendLine("    dependency(     ")
                    extension.rawDependencyConfiguration.forEach { rawDependency ->
                        appendLine(rawDependency.content + ",")
                    }
                    extension.packages.forEach { definition ->
                        when (definition) {
                            is SwiftDependency.Binary.Local -> {
                                appendLine("SwiftDependency.Binary.Local(")
                                append("path = \"${definition.path}\",")
                                append("packageName = \"${definition.packageName}\",")
                                append("exportToKotlin = ${definition.exportToKotlin}")
                                append("),")
                            }

                            is SwiftDependency.Binary.Remote -> {
                                appendLine("SwiftDependency.Binary.Remote(")
                                append("url = URI(\"${definition.url}\"),")
                                append("checksum = \"${definition.checksum}\",")
                                append("packageName = \"${definition.packageName}\",")
                                append("exportToKotlin = ${definition.exportToKotlin}")
                                append("),")
                            }

                            is SwiftDependency.Package.Local -> {
                                appendLine("SwiftDependency.Package.Local(")
                                append("path = \"${definition.path}\",")
                                if (definition.packageName.isNotEmpty()) {
                                    appendLine("packageName = \"${definition.packageName}\",")
                                }
                                appendLine("products = {")
                                definition.productsConfig.productPackages.forEach { config ->
                                    append(
                                        buildPackageBlock(
                                            config = config,
                                        ),
                                    )
                                }
                                appendLine("}")
                                append("),")
                            }

                            is SwiftDependency.Package.Remote.Branch -> {
                                appendLine("SwiftDependency.Package.Remote.Branch(")
                                appendLine("branch = \"${definition.branch}\",")
                                appendLine("url = URI(\"${definition.url}\"),")
                                if (definition.packageName.isNotEmpty()) {
                                    appendLine("packageName = \"${definition.packageName}\",")
                                }
                                appendLine("products = {")
                                definition.productsConfig.productPackages.forEach { config ->
                                    append(
                                        buildPackageBlock(
                                            config = config,
                                        ),
                                    )
                                }
                                appendLine("}")
                                append("),")
                            }

                            is SwiftDependency.Package.Remote.Commit -> {
                                appendLine("SwiftDependency.Package.Remote.Commit(")
                                appendLine("revision = \"${definition.revision}\",")
                                appendLine("url = URI(\"${definition.url}\"),")
                                if (definition.packageName.isNotEmpty()) {
                                    appendLine("packageName = \"${definition.packageName}\",")
                                }
                                appendLine("products = {")
                                definition.productsConfig.productPackages.forEach { config ->
                                    append(
                                        buildPackageBlock(
                                            config = config,
                                        ),
                                    )
                                }
                                appendLine("}")
                                append("),")
                            }

                            is SwiftDependency.Package.Remote.Version -> {
                                appendLine("SwiftDependency.Package.Remote.Version(")
                                appendLine("version = \"${definition.version}\",")
                                appendLine("url = URI(\"${definition.url}\"),")
                                if (definition.packageName.isNotEmpty()) {
                                    appendLine("packageName = \"${definition.packageName}\",")
                                }
                                appendLine("products = {")
                                definition.productsConfig.productPackages.forEach { config ->
                                    append(
                                        buildPackageBlock(
                                            config = config,
                                        ),
                                    )
                                }
                                appendLine("}")
                                append("),")
                            }
                        }
                    }
                    appendLine(")")
                    appendLine("}")
                    appendLine("}")
                }
            }
        val targets = configuration.targets.joinToString(separator = ",") { "$it()" }
        val script =
            """
            // START enable code-coverage

            abstract class JacocoDumper : BuildService<BuildServiceParameters.None>, AutoCloseable {
                override fun close() {
                    val mBeanServer = ManagementFactory.getPlatformMBeanServer()
                    val jacocoObjectName = ObjectName.getInstance("org.jacoco:type=Runtime")
                    if (mBeanServer.isRegistered(jacocoObjectName)) {
                        mBeanServer.invoke(jacocoObjectName, "dump", arrayOf(true), arrayOf("boolean"))
                    }
                }
            }
            val jacocoDumper = gradle.sharedServices.registerIfAbsent("jacocoDumper", JacocoDumper::class) {}
            jacocoDumper.get()
            gradle.allprojects {
                tasks.configureEach {
                    usesService(jacocoDumper)
                }
            }

            // END enable code-coverage

            kotlin {
                listOf(
                   $targets
                ).forEach {
                    it.compilations {
                        val main by getting {
                            cinterops.create("${configuration.cinteropsName}")
                        }
                    }
                    it.binaries.framework {
                        baseName = "shared"
                        isStatic = true
                    }
                }
            }
            $pluginBlock
            """.trimIndent()
        logger.debug(script)
        return script
    }

    class Builder {
        private var config = TestConfiguration()

        fun withBuildPath(buildPath: String) =
            apply {
                config = config.copy(buildPath = buildPath)
            }

        fun withSwiftSources(vararg sources: SwiftSource) =
            apply {
                config = config.copy(swiftSources = sources.toList())
            }

        fun withKotlinSources(vararg sources: KotlinSource) =
            apply {
                config = config.copy(kotlinSources = sources.toList())
            }

        fun withTargets(vararg targets: AppleCompileTarget) =
            apply {
                config = config.copy(targets = targets.toList())
            }

        fun withDependencies(definitions: List<SwiftDependency>) =
            apply {
                config = config.copy(packages = definitions)
            }

        fun withCache(path: String) =
            apply {
                config = config.copy(sharedCachePath = path)
            }

        fun withConfig(path: String) =
            apply {
                config = config.copy(sharedConfigPath = path)
            }

        fun withSecurity(path: String) =
            apply {
                config = config.copy(sharedSecurityPath = path)
            }

        fun withSPMPath(path: String) =
            apply {
                config = config.copy(customSPMPath = path)
            }

        fun withMinMacos(minMacos: String?) =
            apply {
                config = config.copy(minMacos = minMacos)
            }

        fun withMinIos(minIos: String?) =
            apply {
                config = config.copy(minIos = minIos)
            }

        fun withMinWatchOs(minWatchOs: String?) =
            apply {
                config = config.copy(minWatchos = minWatchOs)
            }

        fun withMinTvos(minTvOs: String?) =
            apply {
                config = config.copy(minTvos = minTvOs)
            }

        fun withRawDependencies(vararg sources: KotlinSource) =
            apply {
                config = config.copy(rawDependencyConfiguration = sources.toList())
            }

        fun withRawPluginConfiguration(vararg sources: KotlinSource) =
            apply {
                config = config.copy(rawPluginConfiguration = sources.toList())
            }

        fun withCopyDependenciesToApp(copyDependenciesToApp: Boolean) =
            apply {
                config = config.copy(copyDependenciesToApp = copyDependenciesToApp)
            }

        fun appendRawPluginRootConfig(
            @Language("kotlin") source: String,
        ) = apply {
            config = config.copy(rawPluginRootConfig = source)
        }

        fun withPackageDependencyPrefix(prefix: String?) =
            apply {
                config = config.copy(packageDependencyPrefix = prefix)
            }

        fun build(): SmpKMPTestFixture =
            object : SmpKMPTestFixture(config) {
                override fun createProject(): GradleProject = createDefaultProject(config)
            }
    }

    companion object {
        fun builder() = Builder()
    }
}
