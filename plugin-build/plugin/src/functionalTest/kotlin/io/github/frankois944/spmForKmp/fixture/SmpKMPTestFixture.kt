package io.github.frankois944.spmForKmp.fixture

import com.autonomousapps.kit.AbstractGradleProject
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.RootProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.Subproject
import com.autonomousapps.kit.gradle.Imports
import com.autonomousapps.kit.gradle.Plugin
import io.github.frankois944.spmForKmp.config.AppleCompileTarget
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
        val sharedCachePath: String? = null,
        val sharedConfigPath: String? = null,
        val sharedSecurityPath: String? = null,
        val customSPMPath: String? = null,
        val rawDependencyConfiguration: KotlinSource? = null,
        val rawPluginConfiguration: List<KotlinSource> = emptyList(),
        val rawPluginRootConfig: String? = null,
        val gradleCaching: Boolean = true,
        val rawTargetBloc: KotlinSource? = null,
    )

    protected abstract fun createProject(): GradleProject

    protected fun createDefaultProject(extension: TestConfiguration): GradleProject =
        newGradleProjectBuilder(GradleProject.DslKind.KOTLIN)
            .withRootProject {
                setupProperties(extension)
            }.withSubproject("library") {
                setupSources(extension.cinteropsName)
                setupGradleConfig(extension)
            }.write()

    private fun RootProject.Builder.setupProperties(extension: TestConfiguration) {
        var content = """
kotlin.mpp.enableCInteropCommonization=true
org.gradle.caching=${ if (extension.gradleCaching) "true" else "false" }
spmforkmp.enableTracing=true
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
                    "java.net.URI",
                    "java.lang.management.ManagementFactory",
                    "javax.management.ObjectName",
                    "io.github.frankois944.spmForKmp.definition.product.ProductName",
                    "io.github.frankois944.spmForKmp.swiftPackageConfig",
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
            } else if (configuration.rawTargetBloc == null) {
                buildString {
                    append(
                        """
swiftPackageConfig {
    create("${extension.cinteropsName}") {
    customPackageSourcePath = "${extension.customPackageSourcePath}"
    toolsVersion = "${extension.toolsVersion}"
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
                    extension.rawPluginRootConfig?.let {
                        appendLine(it)
                    }
                    appendLine("    dependency {     ")
                    extension.rawDependencyConfiguration?.let { rawDependency ->
                        appendLine(rawDependency.content)
                    }
                    appendLine("}")
                    appendLine("}")
                    appendLine("}")
                }
            } else {
                ""
            }
        val targets = configuration.targets.joinToString(separator = ",") { "$it()" }
        var script =
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
                    """
        configuration.rawTargetBloc?.let { rawTargetBloc ->
            script += rawTargetBloc.content
        } ?: run {
            script += """

            it.compilations {
                val main by getting {
                    cinterops.create("${configuration.cinteropsName}")
                }
            }
            """
        }
        script +=
            """

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

        fun withRawTargetBlock(rawTargetBloc: KotlinSource) =
            apply {
                config = config.copy(rawTargetBloc = rawTargetBloc)
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

        fun withRawDependencies(sources: KotlinSource) =
            apply {
                config = config.copy(rawDependencyConfiguration = sources)
            }

        fun withRawPluginConfiguration(vararg sources: KotlinSource) =
            apply {
                config = config.copy(rawPluginConfiguration = sources.toList())
            }

        fun withToolsVersion(toolsVersion: String) =
            apply {
                config = config.copy(toolsVersion = toolsVersion)
            }

        fun withGradleCache(isEnable: Boolean = true) =
            apply {
                config = config.copy(gradleCaching = isEnable)
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
