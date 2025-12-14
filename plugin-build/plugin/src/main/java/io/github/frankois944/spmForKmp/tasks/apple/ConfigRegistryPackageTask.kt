package io.github.frankois944.spmForKmp.tasks.apple

import io.github.frankois944.spmForKmp.definition.packageRegistry.auth.PackageRegistryAuth
import io.github.frankois944.spmForKmp.operations.packageRegistryAuth
import io.github.frankois944.spmForKmp.operations.packageRegistrySet
import io.github.frankois944.spmForKmp.tasks.utils.TaskTracer
import org.gradle.api.DefaultTask
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.gradle.internal.ensureParentDirsCreated
import org.jetbrains.kotlin.konan.target.HostManager
import java.io.File
import javax.inject.Inject

@CacheableTask
internal abstract class ConfigRegistryPackageTask : DefaultTask() {
    @get:Internal
    abstract val workingDir: Property<File>

    @get:Input
    abstract val registries: ListProperty<PackageRegistryAuth>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val tokenFile: ListProperty<File>
        get() {
            val files =
                registries.get().mapNotNull { registry ->
                    registry.tokenFile
                }
            logger.debug("Found token files {}", files)
            return project.objects.listProperty(File::class.java).apply {
                addAll(files)
            }
        }

    @get:OutputFile
    @get:Optional
    val registriesFile: File?
        get() {
            // .swiftpm/configuration/registries.json.
            val manifest =
                workingDir
                    .get()
                    .resolve(".swiftpm")
            return if (registries.get().isEmpty()) {
                logger.debug("registries is empty delete .swiftpm if exist")
                if (manifest.exists()) {
                    manifest
                        .deleteRecursively()
                }
                null
            } else {
                manifest
                    .resolve("configuration")
                    .resolve("registries.json")
                    .also {
                        logger.debug("Create swiftpm config directory")
                        it.ensureParentDirsCreated()
                    }
            }
        }

    @get:Input
    @get:Optional
    abstract val swiftBinPath: Property<String?>

    @get:Input
    abstract val traceEnabled: Property<Boolean>

    @get:Internal
    val tracer: TaskTracer by lazy {
        TaskTracer(
            "ConfigRegistryPackageTask",
            traceEnabled.get(),
            outputFile =
                project.projectDir
                    .resolve("spmForKmpTrace")
                    .resolve("ConfigRegistryPackageTask.html"),
        )
    }

    @get:Inject
    abstract val execOps: ExecOperations

    init {
        description = "Generate Package Registry Manifest"
        group = "io.github.frankois944.spmForKmp.tasks"
        onlyIf {
            HostManager.hostIsMac && registries.get().isNotEmpty()
        }
        logger.debug("Found registries {}", registries.get())
    }

    @TaskAction
    fun generateFile() {
        tracer.trace("ConfigRegistryPackageTask") {
            registries.get().forEach { registry ->
                tracer.trace("new package registry ${registry.url}") {
                    logger.debug("Create a new package registry file {}", registry.url)
                    logger.debug("URL : {}", workingDir.get())
                    registriesFile?.let { file ->
                        logger.debug("previous config file exist, delete it")
                        if (file.exists()) {
                            file.delete()
                        }
                    }
                    tracer.trace("packageRegistrySet") {
                        execOps.packageRegistrySet(
                            workingDir = workingDir.get(),
                            url = registry.url,
                            logger = logger,
                            swiftBinPath = swiftBinPath.orNull,
                        )
                    }
                    if (registry.hasAuthCredentials()) {
                        logger.debug("Authenticate with package registry {}", registry.url)
                        logger.debug("username: {} password: {}", registry.username, registry.password)
                        logger.debug("token: {}", tokenFile.orNull)
                        logger.debug("tokenFile: {}", tokenFile.orNull)
                        tracer.trace("packageRegistryAuth") {
                            execOps.packageRegistryAuth(
                                workingDir = workingDir.get(),
                                url = registry.url,
                                username = registry.username,
                                password = registry.password,
                                token = registry.token,
                                tokenFile = registry.tokenFile,
                                logger = logger,
                                swiftBinPath = swiftBinPath.orNull,
                            )
                        }
                    }
                }
            }
        }
        tracer.writeHtmlReport()
    }
}
