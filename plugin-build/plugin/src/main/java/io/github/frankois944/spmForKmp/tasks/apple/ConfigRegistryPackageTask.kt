package io.github.frankois944.spmForKmp.tasks.apple

import io.github.frankois944.spmForKmp.definition.packageRegistry.auth.PackageRegistryAuth
import io.github.frankois944.spmForKmp.operations.packageRegistryAuth
import io.github.frankois944.spmForKmp.operations.packageRegistrySet
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
import kotlin.math.log

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
            logger.warn(">>><<<<Found token files {}", registries.get())
            val files =
                registries.get().mapNotNull { registry ->
                    registry.tokenFile
                }
            logger.warn("Found token files {}", files)
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
                logger.warn("registries is empty delete .swiftpm if exist")
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
                        logger.warn("Create swiftpm config directory")
                        it.ensureParentDirsCreated()
                    }
            }
        }

    @get:Input
    @get:Optional
    abstract val swiftBinPath: Property<String?>

    @get:Inject
    abstract val execOps: ExecOperations

    init {
        description = "Generate Package Registry Manifest"
        group = "io.github.frankois944.spmForKmp.tasks"
        onlyIf {
            HostManager.hostIsMac
        }
        logger.warn("Found registries {}", registries.get())
    }

    @TaskAction
    fun generateFile() {
        registries.get().forEach { registry ->
            logger.warn("Create a new package registry file {}", registry.url)
            logger.warn("URL : {}", workingDir.get())
            registriesFile?.let { file ->
                if (file.exists()) {
                    file.delete()
                }
            }
            execOps.packageRegistrySet(
                workingDir = workingDir.get(),
                url = registry.url,
                logger = logger,
                swiftBinPath = swiftBinPath.orNull,
            )
            if ((registry.username != null && registry.password != null) ||
                registry.token != null ||
                registry.tokenFile != null
            ) {
                logger.warn("Authenticate with package registry {}", registry.url)
                logger.warn("username: {} password: {}", registry.username, registry.password)
                logger.warn("token: {}", tokenFile.orNull)
                logger.warn("tokenFile: {}", tokenFile.orNull)
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
