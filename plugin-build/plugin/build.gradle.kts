import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    `java-gradle-plugin`
    alias(libs.plugins.pluginPublish)
    alias(libs.plugins.autonomousapps.testkit)
    // id("io.kotest")
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(gradleApi())
    implementation(libs.kotlin.gradle)

    // testImplementation(gradleTestKit())
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("io.kotest:kotest-assertions-core-jvm:6.0.0.M1")
    testImplementation("io.kotest:kotest-framework-engine-jvm:6.0.0.M1")
    testImplementation("io.kotest:kotest-runner-junit5:6.0.0.M1")

    // functionalTestImplementation(gradleTestKit())
    functionalTestImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    functionalTestRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    functionalTestRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

gradleTestKitSupport {
    withSupportLibrary()
    withTruthLibrary()
}

tasks.named<Test>("functionalTest") {
    useJUnitPlatform()
    systemProperty("com.autonomousapps.test.versions.kotlin", libs.versions.kotlin.get())
    systemProperty("org.gradle.testkit.debug", true)
    debug = true
    beforeTest(
        closureOf<TestDescriptor> {
            logger.warn("Running functionalTest: $this")
        },
    )
}

tasks.named<Test>("test") {
    println("SETUP test")
    useJUnitPlatform()
    systemProperty("com.autonomousapps.test.versions.kotlin", libs.versions.kotlin.get())
    systemProperty("org.gradle.testkit.debug", true)
    debug = true
    beforeTest(
        closureOf<TestDescriptor> {
            logger.warn("Running test: $this")
        },
    )
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

        if (key == null || secret == null) {
            throw GradleException("gradlePublishKey and/or gradlePublishSecret are not defined environment variables")
        }

        System.setProperty("gradle.publish.key", key)
        System.setProperty("gradle.publish.secret", secret)
    }
}
