
import io.github.frankois944.spmForKmp.definition.SwiftDependency
import io.github.frankois944.spmForKmp.definition.product.ProductName
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
        // iosArm64(),
        iosSimulatorArm64(),
    ).forEach {
        it.binaries.framework {
            baseName = "shared"
            isStatic = true
        }
        it.compilations {
            val main by getting {
                cinterops.create("nativeShared")
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
val testRessources = "${layout.projectDirectory.asFile.path}/../plugin-build/plugin/src/functionalTest/resources"
swiftPackageConfig {
    create("nativeShared") {
        // optional parameters
        // the ios minimal version
        // minIos = "12.0"
        // the tvos minimal version
        // minTvos = "12.0"
        // the watchos minimal version
        // minWatchos = "12.0"
        // the macos minimal version
        minMacos = "10.15"
        // the directory where your own swift code is located
        // customPackageSourcePath = "{buildDir}/src/swift"
        // the swift code is built in debug by default
        // debug = false :
        dependency(
            SwiftDependency.Package.Remote.Version(
                // Repository URL
                url = URI("https://github.com/firebase/firebase-ios-sdk.git"),
                // Libraries from the package
                products = {
                    // Export to Kotlin for use in shared Kotlin code
                    add("FirebaseCore", "FirebaseAnalytics", exportToKotlin = true)
                    // add FirebaseDatabase to your own swift code but don't export it
                    add(ProductName("FirebaseDatabase"))
                },
                // (Optional) Package name, can be required in some cases
                packageName = "firebase-ios-sdk",
                // Package version
                version = "11.6.0",
            ),
            SwiftDependency.Binary.Local(
                path = "$testRessources/DummyFramework.xcframework.zip",
                packageName = "DummyFramework",
                exportToKotlin = true,
            ),
            SwiftDependency.Package.Local(
                path = "$testRessources/LocalSourceDummyFramework",
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
}
