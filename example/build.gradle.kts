import io.github.frankois944.spmForKmp.definition.product.ProductName
import io.github.frankois944.spmForKmp.swiftPackageConfig

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    id("io.github.frankois944.spmForKmp")
}

val testResources = "${layout.projectDirectory.asFile.path}/../plugin-build/plugin/src/functionalTest/resources"

kotlin {

    listOf(
        iosArm64(),
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
        /**
         * cinteropName is only needed when using a list of target
         * Or when you want to keep compatibility with older versions of the plugin
         */
        target.swiftPackageConfig(cinteropName = "nativeIosShared") {
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
            spmWorkingPath = "${projectDir.resolve("SPM")}" // change the Swift Package Manager working Dir
            // spmWorkingPath = "${projectDir.resolve("SPM")}" // change the Swift Package Manager working Dir
            // swiftBinPath = "/path/to/.swiftly/bin/swift"
            // exportedPackageSettings {
            //     includeProduct = listOf("HevSocks5Tunnel")
            // }
            minIos = "16.0"
            dependency {
                remotePackageVersion(
                    url = uri("https://github.com/firebase/firebase-ios-sdk.git"),
                    // Libraries from the package
                    products = {
                        // Export to Kotlin for use in shared Kotlin code
                        add("FirebaseAnalytics", exportToKotlin = true)
                        add(ProductName("FirebaseCore"), exportToKotlin = true)
                        // add FirebaseDatabase to your own swift code but don't export it
                        add(ProductName("FirebaseDatabase"))
                    },
                    // (Optional) Package name, can be required in some cases
                    packageName = "firebase-ios-sdk",
                    // Package version
                    version = "12.3.0",
                )
                localBinary(
                    path = "$testResources/DummyFrameworkV2.xcframework.zip",
                    packageName = "DummyFramework",
                    exportToKotlin = true,
                )
                localBinary(
                    path = "${layout.projectDirectory.asFile.path}/../example/xcframework/Sentry-Dynamic.xcframework.zip",
                    packageName = "Sentry",
                    exportToKotlin = false,
                )
                localPackage(
                    path = "$testResources/LocalSourceDummyFramework",
                    packageName = "LocalSourceDummyFramework",
                    products = {
                        // Export to Kotlin for use in shared Kotlin code, false by default
                        add(
                            "LocalSourceDummyFramework",
                            exportToKotlin = true,
                        )
                    },
                )
                remotePackageVersion(
                    url = uri("https://github.com/krzyzanowskim/CryptoSwift.git"),
                    version = "1.8.1",
                    products = {
                        // Can be only used in your "src/swift" code.
                        add("CryptoSwift")
                    },
                )
                remoteBinary(
                    url = uri("https://github.com/wanliyunyan/HevSocks5Tunnel/releases/download/2.10.0/HevSocks5Tunnel.xcframework.zip"),
                    packageName = "HevSocks5Tunnel",
                    exportToKotlin = true,
                    checksum = "f66fc314edbdb7611c5e8522bc50ee62e7930f37f80631b8d08b2a40c81a631a",
                    isCLang = true,
                )
                remotePackageVersion(
                    url = uri("https://github.com/SDWebImage/SDWebImage.git"),
                    products = {
                        add("SDWebImage")
                    },
                    version = "5.21.3",
                )
            }
        }
    }

    macosArm64 {
        binaries.getTest("debug").apply {
            linkerOpts +=
                listOf(
                    "-rpath",
                    "${projectDir.path}/build/spmKmpPlugin/nativeMacosShared/scratch/arm64-apple-macosx/release/",
                )
        }
        binaries.framework {
            baseName = "shared"
            isStatic = true
        }
        swiftPackageConfig(cinteropName = "nativeMacosShared") {
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
    }

    sourceSets {
        commonMain.dependencies {
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
        }
    }
}
