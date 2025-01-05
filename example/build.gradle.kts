import io.github.frankois944.spmForKmp.definition.SwiftDependency
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

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
        iosArm64(),
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

swiftPackageConfig {
    create("nativeShared") {
        // optional parameters
        // minIos = "12.0" : the ios minimal version
        // minTvos = "12.0" : the tvos minimal version
        // minWatchos = "12.0" : the watchos minimal version
        // minMacos = "10.13" : the macos minimal version
        // customPackageSourcePath = "{buildDir}/src/swift" : the directory where your own swift code is located
        // debug = false : the swift code is build in debug by default
        dependency(
            SwiftDependency.Package.Remote.Version(
                // Repository URL
                url = "https://github.com/firebase/firebase-ios-sdk.git",
                // Libraries from the package
                names = listOf("FirebaseAnalytics", "FirebaseCore"),
                // (Optional) Package name, can be required in some cases
                packageName = "firebase-ios-sdk",
                // Package version
                version = "11.6.0",
                // Export to Kotlin for use in shared Kotlin code, false by default
                exportToKotlin = true,
            ),
            SwiftDependency.Package.Remote.Version(
                url = "https://github.com/krzyzanowskim/CryptoSwift.git",
                names = listOf("CryptoSwift"),
                version = "1.8.1",
            ),
            // see SwiftDependency class for more use cases
        )
    }
}
