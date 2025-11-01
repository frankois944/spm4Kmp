package io.github.frankois944.spmForKmp.tasks.apple

import io.github.frankois944.spmForKmp.operations.isDynamicLibrary
import io.github.frankois944.spmForKmp.operations.signFramework
import io.github.frankois944.spmForKmp.resources.CopiedResourcesFactory
import io.github.frankois944.spmForKmp.resources.FrameworkResource
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.jetbrains.kotlin.konan.target.HostManager
import java.io.File
import javax.inject.Inject

@CacheableTask
internal abstract class CopyPackageResourcesTask : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val builtDirectory: DirectoryProperty

    @get:Input
    @get:Optional
    abstract val codeSignIdentityName: Property<String?>

    @get:Input
    abstract val buildProductDir: Property<String>

    @get:Input
    abstract val contentFolderPath: Property<String>

    @get:Inject
    abstract val execOps: ExecOperations

    init {
        description = "Copy package resource to application"
        group = "io.github.frankois944.spmForKmp.tasks"
        onlyIf {
            HostManager.hostIsMac
        }
    }

    private companion object {
        val EXCLUDED_FRAMEWORK_DIRS = setOf("Modules", "Headers")
    }

    @TaskAction
    fun copyResources() {
        logger.debug("preparing resources")
        val copiedResources = createCopiedResources()

        copyBundleResources(copiedResources)
        copyFrameworkResources(copiedResources)
    }

    private fun createCopiedResources(): CopiedResourcesFactory =
        CopiedResourcesFactory(
            inputBuiltDirectory = builtDirectory.get().asFile,
            contentFolderPath = contentFolderPath.get(),
            buildProductDir = buildProductDir.get(),
            logger = logger,
        )

    private fun copyBundleResources(copiedResources: CopiedResourcesFactory) {
        logger.debug("Start copy bundle resources")
        copiedResources.bundles.forEach { bundle ->
            val destination = File(copiedResources.outputBundleDirectory, bundle.name)
            logger.debug("copy resources bundle {} to {}", bundle.absolutePath, destination.absolutePath)
            bundle.copyRecursively(destination, overwrite = true, followLinks = true)
        }
        logger.debug("End copy bundle resources")
    }

    private fun copyFrameworkResources(copiedResources: CopiedResourcesFactory) {
        logger.debug("Start copy framework resources")

        copiedResources.outputFrameworkDirectory.parentFile.parentFile.let { targetDir ->
            copyDynamicFrameworksTo(
                targetDir = targetDir,
                copiedResources = copiedResources,
                canSignFrameworkResource = false,
            )
        }

        copyDynamicFrameworksTo(
            targetDir = copiedResources.outputFrameworkDirectory,
            copiedResources = copiedResources,
            canSignFrameworkResource = true,
        )
        logger.debug("End copy framework resources")
    }

    private fun copyDynamicFrameworksTo(
        targetDir: File,
        copiedResources: CopiedResourcesFactory,
        canSignFrameworkResource: Boolean,
    ) {
        copiedResources.frameworks
            .filter { isDynamicFramework(it) }
            .onEach { collectFrameworkFiles(it) }
            .forEach { framework ->
                copyFrameworkFiles(framework, targetDir)
                if (canSignFrameworkResource) {
                    signFrameworkResources(targetDir.resolve(framework.name))
                }
            }
    }

    private fun signFrameworkResources(file: File) {
        codeSignIdentityName.orNull?.let { identity ->
            if (identity == "Sign to Run Locally" || identity.isEmpty()) {
                logger.debug("Ignore framework signing because of local run")
                return@let
            }
            logger.debug("Found sign identity {}", identity)
            logger.debug("Signing framework at {}", file)
            execOps.signFramework(
                file = file,
                logger = logger,
                signIdentityName = identity,
            )
        } ?: run {
            logger.debug("Can't resign framework, no signature identity found (EXPANDED_CODE_SIGN_IDENTITY_NAME)")
        }
    }

    private fun isDynamicFramework(framework: FrameworkResource): Boolean {
        if (!execOps.isDynamicLibrary(framework.binaryFile, logger)) {
            logger.debug("Ignore {} because it is not a dynamic library", framework.binaryFile)
            return false
        }
        return true
    }

    private fun collectFrameworkFiles(frameworkResource: FrameworkResource) {
        frameworkResource.framework.listFiles()?.forEach { file ->
            when {
                !file.isDirectory -> frameworkResource.files.add(file)
                !EXCLUDED_FRAMEWORK_DIRS.contains(file.name) -> frameworkResource.files.add(file)
            }
        }
    }

    private fun copyFrameworkFiles(
        framework: FrameworkResource,
        targetDir: File,
    ) {
        val destination =
            File(targetDir, framework.name).apply {
                if (!exists()) mkdirs()
            }

        logger.debug("copy framework {} to {}", framework.name, destination)
        framework.files.forEach { file ->
            val destFile = destination.resolve(file.name)
            logger.debug("copy framework file {} to {}", file.name, destFile)
            file.copyRecursively(destFile, overwrite = true, followLinks = true)
        }
    }
}
