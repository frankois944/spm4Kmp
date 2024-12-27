package fr.frankois944.spm.kmp.plugin

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import java.io.File

public val pluginName = "fr.frankois944.spm.kmp.plugin"

public val gradleFile = "build.gradle.kts"

public fun executeGradleRun(
    task: String,
    testProjectDir: File,
): BuildResult =
    GradleRunner
        .create()
        .withProjectDir(testProjectDir)
        .withArguments(task)
        .withPluginClasspath()
        .build()

public fun generateBuildFile(config: String) =
    """
    import fr.frankois944.spm.kmp.plugin.definition.SwiftPackageDependencyDefinition
    plugins {
        id("$pluginName")
    }
    swiftPackageConfig {
        $config
    }
    """.trimIndent()

public fun packageTestContent1(path: String) =
    """
    customPackageSourcePath = "$path/src/spm"
    minIos = "14.0"
    minMacos = "10.15"
    packages.add(
        SwiftPackageDependencyDefinition.RemoteDefinition.Version(
            url = "https://github.com/krzyzanowskim/CryptoSwift.git",
            names = listOf("CryptoSwift"),
            version = "1.8.3",
        ),
    )
    """.trimIndent()

public fun packageTestContent2(path: String) =
    """
    customPackageSourcePath = "$path/src/spm"
    minIos = "14.0"
    minMacos = "10.15"
    packages.add(
        SwiftPackageDependencyDefinition.RemoteDefinition.Version(
            url = "https://github.com/krzyzanowskim/CryptoSwift.git",
            names = listOf("CryptoSwift"),
            version = "1.8.3",
        ),
    )
    packages.add(
        SwiftPackageDependencyDefinition.RemoteDefinition.Version(
            url = "https://github.com/firebase/firebase-ios-sdk",
            names = listOf("FirebaseCore", "FirebaseAnalytics", "FirebaseCrashlytics"),
            packageName = "firebase-ios-sdk",
            version = "11.6.0",
        ),
    )
    """.trimIndent()
