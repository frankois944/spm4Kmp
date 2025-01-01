import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    `java-gradle-plugin`
    alias(libs.plugins.pluginPublish)
    alias(libs.plugins.autonomousapps.testkit)
    alias(libs.plugins.publish)
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(gradleApi())
    implementation(libs.kotlin.gradle)

    functionalTestImplementation(libs.junit.jupiter)
    functionalTestRuntimeOnly(libs.junit.jupiter.engine)
    functionalTestRuntimeOnly(libs.junit.platform.launcher)
    functionalTestImplementation(project(":plugin"))
}

gradleTestKitSupport {
    withSupportLibrary()
    withTruthLibrary()
}

tasks.named<Test>("functionalTest") {
    useJUnitPlatform()
    systemProperty("com.autonomousapps.test.versions.kotlin", libs.versions.kotlin.get())
    systemProperty("org.gradle.testkit.debug", true)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    explicitApi()
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
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
            tags.set(listOf("kmp", "spm", "cinterp", "apple"))
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

tasks.create("setupPluginUploadFromEnvironment") {
    doLast {
        val key = System.getenv("GRADLE_PUBLISH_KEY")
        val secret = System.getenv("GRADLE_PUBLISH_SECRET")

        val mavenKey = System.getenv("MAVEN_PUBLISH_KEY")
        val mavenSecret = System.getenv("MAVEN_PUBLISH_SECRET")
        val signingKeyId = System.getenv("SIGNING_KEY_ID")
        val signingKeyPassword = System.getenv("SIGNING_KEY_PASSWORD")
        val signingFile = System.getenv("SIGNING_FILE")

        if (key == null || secret == null) {
            throw GradleException("gradlePublishKey and/or gradlePublishSecret are not defined environment variables")
        }

        System.setProperty("gradle.publish.key", key)
        System.setProperty("gradle.publish.secret", secret)
        System.setProperty("mavenCentralUsername", mavenKey ?: "")
        System.setProperty("mavenCentralPassword", mavenSecret ?: "")
        System.setProperty("signing.keyId", signingKeyId ?: "")
        System.setProperty("signing.password", signingKeyPassword ?: "")
        System.setProperty("signing.secretKeyRingFile", signingFile ?: "")
    }
}

mavenPublishing {
    // Define coordinates for the published artifact
    coordinates(
        groupId = property("GROUP").toString(),
        artifactId = "SpmForKmp",
        version = property("VERSION").toString(),
    )

    // Configure POM metadata for the published artifact
    pom {
        name.set(property("DISPLAY_NAME").toString())
        description.set(
            property("DESCRIPTION").toString(),
        )
        inceptionYear.set("2025")
        url.set(property("WEBSITE").toString())

        licenses {
            license {
                name.set("MIT")
                url.set(property("WEBSITE").toString())
            }
        }

        // Specify developer information
        developers {
            developer {
                id.set("frankois944")
                name.set("Francois Dabonot")
                email.set("dabonot.francois@gmail.com")
            }
        }

        // Specify SCM information
        scm {
            url.set(property("VCS_URL").toString())
        }
    }

    // Configure publishing to Maven Central
    publishToMavenCentral(com.vanniktech.maven.publish.SonatypeHost.CENTRAL_PORTAL)

    // Enable GPG signing for all publications
    signAllPublications()
}
