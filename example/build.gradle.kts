import io.github.frankois944.spmForKmp.definition.SwiftDependency
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.net.URI

plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinMultiplatform)
    id("io.github.frankois944.spmForKmp")
}

kotlin {
    androidTarget {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_1_8)
                }
            }
        }
    }

    listOf(
        iosX64(),
        iosSimulatorArm64(),
    ).forEach {
        it.binaries.framework {
            baseName = "shared"
            isStatic = true
        }
        it.compilations {
            val main by getting {
                cinterops.create("nativeIosShared")
            }
        }
    }

    listOf(
     //   linuxArm64(),
        linuxX64(),
    ).forEach {
        it.compilations {
            val main by getting {
                cinterops.create("nativeLinuxShared")
            }
        }
    }

    listOf(
        macosArm64(),
    ).forEach {
        it.binaries.framework {
            baseName = "shared"
            isStatic = true
        }
        it.compilations {
            val main by getting {
                cinterops.create("nativeMacosShared")
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

val copyTestResources =
    tasks.register<Copy>("copyTestResources") {
        from(
            "${layout.projectDirectory.asFile.path}/../plugin-build/plugin/src/functionalTest/resources" +
                "/DummyFramework.xcframework/ios-arm64_x86_64-simulator/",
        ) {
            include("*.framework/**")
        }
        into("${layout.projectDirectory.asFile.path}/build/bin/iosSimulatorArm64/debugTest/Frameworks/")
    }

tasks.named("iosSimulatorArm64Test") {
    dependsOn(copyTestResources)
}

android {
    namespace = "com.example"
    compileSdk = 34
    defaultConfig {
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}
val testResources = "${layout.projectDirectory.asFile.path}/../plugin-build/plugin/src/functionalTest/resources"
swiftPackageConfig {
    create("nativeIosShared") {
        // optional parameters
        // the ios minimal version
        // minIos = "12.0"
        // the tvos minimal version
        // minTvos = "12.0"
        // the watchos minimal version
        // minWatchos = "12.0"
        // the macos minimal version
        minMacos = "10.15"
        minIos = "14.0"
        // the directory where your own swift code is located
        // customPackageSourcePath = "{buildDir}/src/swift"
        // the swift code is built in debug by default
        // debug = false :
        // add a prefix to the dependencies package names
        // ei :
        //  - packageDependencyPrefix = "customName"
        //  - give : "customName.FirebaseCore" instead of "FirebaseCore"
        // packageDependencyPrefix = null // default null
        dependency(
            SwiftDependency.Binary.Local(
                path = "$testResources/DummyFramework.xcframework.zip",
                packageName = "DummyFramework",
                exportToKotlin = true,
            ),
            SwiftDependency.Package.Local(
                path = "$testResources/LocalSourceDummyFramework",
                packageName = "LocalSourceDummyFramework",
                products = {
                    // Export to Kotlin for use in shared Kotlin code, false by default
                    add("LocalSourceDummyFramework", exportToKotlin = true)
                },
            ),
            SwiftDependency.Package.Remote.Version(
                url = URI("https://github.com/krzyzanowskim/CryptoSwift.git"),
                version = "1.8.1",
                products = {
                    // Can be only used in your "src/swift" code.
                    add("CryptoSwift")
                },
            ),
            // see SwiftDependency class for more use cases
        )
    }
    create("nativeMacosShared") {
        dependency(
            SwiftDependency.Package.Local(
                path = "$testResources/LocalSourceDummyFramework",
                packageName = "LocalSourceDummyFramework",
                products = {
                    // Export to Kotlin for use in shared Kotlin code, false by default
                    add("LocalSourceDummyFramework", exportToKotlin = false)
                },
            ),
        )
    }

    create("nativeLinuxShared") {
        dependency(
            SwiftDependency.Package.Remote.Version(
                url = URI("https://github.com/apple/swift-crypto"),
                version = "3.10.2",
                products = {
                    add("Crypto")
                },
            ),
        )
    }
}
