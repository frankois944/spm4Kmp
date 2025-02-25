import org.gradle.plugin.devel.tasks.PluginUnderTestMetadata.IMPLEMENTATION_CLASSPATH_PROP_KEY
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.Serializable
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption
import java.util.Properties

plugins {
    kotlin("jvm")
    `java-gradle-plugin`
    alias(libs.plugins.pluginPublish)
    alias(libs.plugins.autonomousapps.testkit)
    jacoco
}

val jacocoAgentJar: Configuration by configurations.creating
jacocoAgentJar.isCanBeResolved = true

dependencies {
    implementation(kotlin("stdlib"))
    implementation(gradleApi())
    implementation(libs.kotlin.gradle)
    implementation(libs.jackson.databind)
    implementation(libs.jackson.module.kotlin)

    testImplementation(kotlin("test"))

    functionalTestImplementation(libs.junit.jupiter)
    functionalTestRuntimeOnly(libs.junit.jupiter.engine)
    functionalTestRuntimeOnly(libs.junit.platform.launcher)
    functionalTestImplementation(project(":plugin"))
    jacocoAgentJar("org.jacoco:org.jacoco.agent:0.8.12:runtime")
}

gradleTestKitSupport {
    withSupportLibrary()
    withTruthLibrary()
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    explicitApi()
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

gradlePlugin {
    plugins {
        create(property("ID").toString()) {
            id = property("ID").toString()
            implementationClass = property("IMPLEMENTATION_CLASS").toString()
            version = property("VERSION").toString()
            description = property("DESCRIPTION").toString()
            displayName = property("DISPLAY_NAME").toString()
            // Note: tags cannot include "plugin" or "gradle" when publishing
            tags.set(listOf("kmp", "SPM", "cinterop", "apple", "multiplatform", "ios", "swiftpackagemanager"))
        }
    }
}

gradlePlugin {
    website.set(property("WEBSITE").toString())
    vcsUrl.set(property("VCS_URL").toString())
}

// Use Detekt with type resolution for check
tasks.named("check").configure {
    this.setDependsOn(
        this.dependsOn.filterNot {
            it is TaskProvider<*> && it.name == "detekt"
        } + tasks.named("detektMain"),
    )
}

tasks.register("setupPluginUploadFromEnvironment") {
    description = "Setup the 'pluginUpload' credentials from environment variables"
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    doLast {
        val key = System.getenv("GRADLE_PUBLISH_KEY")
        val secret = System.getenv("GRADLE_PUBLISH_SECRET")

        if (key == null || secret == null) {
            throw GradleException("gradlePublishKey and/or gradlePublishSecret are not defined environment variables")
        }
        System.setProperty("gradle.publish.key", key)
        System.setProperty("gradle.publish.secret", secret)
    }
}

tasks.named<Test>("functionalTest") {
    useJUnitPlatform()
    systemProperty("com.autonomousapps.test.versions.kotlin", libs.versions.kotlin.get())
    systemProperty("org.gradle.testkit.debug", true)

    // add jacoco on functionalTest
    // https://github.com/gradle/gradle/issues/1465
    // https://github.com/tomkoptel/jacoco-gradle-testkit/blob/develop/build.gradle.kts
    val jacocoTaskExtension = the<JacocoTaskExtension>()

    finalizedBy(tasks.jacocoTestReport)
    val testRuns = layout.buildDirectory.dir("functionalTest")
    systemProperty("testEnv.workDir", LazyString(testRuns.map { it.asFile.apply { mkdirs() }.absolutePath }))

    val jacocoAgentJar = jacocoAgentJar.singleFile.absolutePath

    // Set system properties for the test task
    systemProperty("jacocoAgentJar", jacocoAgentJar)
    systemProperty("jacocoDestfile", jacocoTaskExtension.destinationFile!!.absolutePath)

    // Add doLast action for read lock
    doLast {
        val jacocoDestfile = jacocoTaskExtension.destinationFile!!
        FileChannel.open(jacocoDestfile.toPath(), StandardOpenOption.READ).use {
            it.lock(0, Long.MAX_VALUE, true).release()
        }
    }
}

tasks.named<Test>("functionalTest") {
    the<JacocoTaskExtension>().excludes = listOf("*")
}

// add jacoco on functionalTest
// https://github.com/gradle/gradle/issues/1465
// https://github.com/tomkoptel/jacoco-gradle-testkit/blob/develop/build.gradle.kts
val disableFix: String? by project
val shouldDisableFix = disableFix?.toBoolean() ?: false
if (!shouldDisableFix) {
    val jacocoAnt by configurations.existing
    tasks.pluginUnderTestMetadata {
        inputs.files(jacocoAnt).withPropertyName("jacocoAntPath").withNormalizer(ClasspathNormalizer::class.java)
        actions.clear()
        doLast {
            val jacocoAntPath = inputs.files.asPath
            val instrumentedPluginClasspath = temporaryDir.resolve("instrumentedPluginClasspath")
            instrumentedPluginClasspath.deleteRecursively()
            ant.withGroovyBuilder {
                "taskdef"(
                    "name" to "instrument",
                    "classname" to "org.jacoco.ant.InstrumentTask",
                    "classpath" to jacocoAntPath,
                )
                "instrument"("destdir" to instrumentedPluginClasspath) {
                    pluginClasspath.asFileTree.visit {
                        "gradleFileResource"(
                            "file" to file.absolutePath.replace("$", "$$"),
                            "name" to relativePath.pathString.replace("$", "$$"),
                        )
                    }
                }
            }

            val properties = Properties()
            if (!pluginClasspath.isEmpty) {
                properties.setProperty(
                    IMPLEMENTATION_CLASSPATH_PROP_KEY,
                    listOf(
                        instrumentedPluginClasspath
                            .absoluteFile
                            .invariantSeparatorsPath,
                        *instrumentedPluginClasspath
                            .listFiles { _, name -> name.endsWith(".jar") }!!
                            .map { it.absoluteFile.invariantSeparatorsPath }
                            .toTypedArray(),
                    ).joinToString(File.pathSeparator),
                )
            }
            outputDirectory.file(PluginUnderTestMetadata.METADATA_FILE_NAME).get().asFile.outputStream().use {
                properties.store(it, null)
            }
        }
    }
}

tasks.jacocoTestReport {
    reports {
        html.required.set(true)
        xml.required.set(true)
        csv.required.set(false)
    }
    executionData(files(tasks.withType<Test>()).filter { it.name.endsWith(".exec") && it.exists() })
    dependsOn(tasks.named<Test>("functionalTest"))
}

class LazyString(
    private val source: Lazy<String>,
) : Serializable {
    constructor(source: () -> String) : this(lazy(source))
    constructor(source: Provider<String>) : this(source::get)

    override fun toString() = source.value
}
