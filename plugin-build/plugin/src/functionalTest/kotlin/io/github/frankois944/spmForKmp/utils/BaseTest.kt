package io.github.frankois944.spmForKmp.utils

import com.autonomousapps.kit.GradleBuilder
import io.github.frankois944.spmForKmp.utils.BaseTest
import io.github.frankois944.spmForKmp.utils.OpenFolderOnFailureExtension
import org.gradle.internal.impldep.org.junit.Rule
import org.gradle.internal.impldep.org.junit.rules.TemporaryFolder
import org.gradle.testkit.runner.GradleRunner
import org.gradle.util.GradleVersion
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.RegisterExtension
import java.io.File

open class BaseTest {
    val jacocoAgentJar: String? get() = System.getProperty("jacocoAgentJar")
    var folderTopOpen: String? = null

    @Rule
    @JvmField
    val testProjectDir =
        object : TemporaryFolder(System.getProperty("testEnv.workDir").let(::File)) {
            override fun after() = Unit
        }

    @BeforeEach
    fun setupTemporaryFolder() {
        testProjectDir.create()
    }

    @RegisterExtension
    @JvmField
    val openFolderOnFailure =
        OpenFolderOnFailureExtension {
            folderTopOpen ?: ""
        }

    @BeforeEach
    fun resetFolderTopOpen() {
        folderTopOpen = null
    }

    fun GradleBuilder.runner(
        path: File,
        command: String,
    ): GradleRunner {
        folderTopOpen = path.absolutePath
        return runner(GradleVersion.current(), path, command)
            .withPluginClasspath()
            .run {
                jacocoAgentJar?.let { agent ->
                    withPluginClasspath(pluginClasspath + File(agent))
                }
            }!!
    }
}
