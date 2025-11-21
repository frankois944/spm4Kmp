import io.github.frankois944.spmForKmp.swiftPackage

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    id("io.github.frankois944.spmForKmp")
}

kotlin {

    listOf(
        // iosArm64(),
        iosSimulatorArm64(),
    ).forEach { target ->
        target.binaries.getTest("debug").apply {
            val scratchDir =
                if (target.name == "iosSimulatorArm64") {
                    "arm64-apple-ios-simulator"
                } else {
                    "arm64-apple-ios"
                }
            val artifactDir =
                if (target.name == "iosSimulatorArm64") {
                    "ios-arm64_x86_64-simulator"
                } else {
                    "ios-arm64"
                }
            linkerOpts +=
                listOf(
                    "-rpath",
                    "${projectDir.path}/SPM/spmKmpPlugin/nativeIosShared/scratch/$scratchDir/release/",
                    "-L${projectDir.path}/SPM/spmKmpPlugin/nativeIosShared/scratch/artifacts/nativeiosshared/HevSocks5Tunnel/HevSocks5Tunnel.xcframework/$artifactDir/",
                    "-lhev-socks5-tunnel",
                )
            freeCompilerArgs +=
                listOf(
                    "-Xoverride-konan-properties=osVersionMin.ios_simulator_arm64=16.0",
                )
        }
        target.binaries.framework {
            baseName = "shared"
            isStatic = true
        }
        target.swiftPackage {
            minIos = "16.0"
            spmWorkingPath =
                project.layout.projectDirectory
                    .dir("SPM")
                    .asFile.path
            dependency {
                remotePackageVersion(
                    url = uri("https://github.com/firebase/firebase-ios-sdk.git"),
                    version = "12.3.0",
                    packageName = "firebase-ios-sdk",
                    products = { add("FirebaseAnalytics", exportToKotlin = true) },
                )
            }
        }
    }

    /*listOf(
        macosArm64(),
    ).forEach { target ->
        target.binaries.getTest("debug").apply {
            linkerOpts +=
                listOf(
                    "-rpath",
                    "${projectDir.path}/build/spmKmpPlugin/nativeMacosShared/scratch/arm64-apple-macosx/release/",
                )
        }
        target.binaries.framework {
            baseName = "shared"
            isStatic = true
        }
        target.swiftPackage {
            dependency {
                localPackage(
                    path = "$testResources/LocalSourceDummyFramework",
                    packageName = "LocalSourceDummyFramework",
                    products = {
                        // Export to Kotlin for use in shared Kotlin code, false by default
                        add("LocalSourceDummyFramework", exportToKotlin = false)
                    },
                )
            }
        }
    }*/

    sourceSets {
        commonMain.dependencies {
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
        }
    }
}

val testResources = "${layout.projectDirectory.asFile.path}/../plugin-build/plugin/src/functionalTest/resources"
/*swiftPackageConfig {
    create("nativeIosShared") {
        // optional parameters
        // the ios minimal version
        // minIos = "12.0"
        // the tvos minimal version
        // minTvos = "12.0"
        // the watchos minimal version
        // minWatchos = "12.0"
        // the macos minimal version
        // minMacos = "10.15"
        // the directory where your own swift code is located
        // customPackageSourcePath = "{buildDir}/src/swift"
        // the swift code is built in debug by default
        // debug = false :
        // add a prefix to the dependencies package names
        // ei :
        //  - packageDependencyPrefix = "customName"
        //  - give : "customName.FirebaseCore" instead of "FirebaseCore"
        // packageDependencyPrefix = null // default null
        // spmWorkingPath = "${projectDir.resolve("SPM")}" // change the Swift Package Manager working Dir
        // swiftBinPath = "/path/to/.swiftly/bin/swift"
        // exportedPackageSettings {
        //     includeProduct = listOf("HevSocks5Tunnel")
        // }
    }
    create("nativeMacosShared") {
        dependency {
            localPackage(
                path = "$testResources/LocalSourceDummyFramework",
                packageName = "LocalSourceDummyFramework",
                products = {
                    // Export to Kotlin for use in shared Kotlin code, false by default
                    add("LocalSourceDummyFramework", exportToKotlin = false)
                },
            )
        }
    }
}*/
