plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("fr.frankois944.spm.kmp.plugin")
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach {
        it.compilations {
            val main by getting {
                cinterops.create("nativeExample")
            }
        }
    }
}

swiftPackageConfig {
    cinteropsName = "nativeExample"
}
