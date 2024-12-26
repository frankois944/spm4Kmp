pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
        mavenCentral()
    }

    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

plugins {
    id("com.gradle.develocity") version "3.19"
}

develocity {
    buildScan.termsOfUseUrl = "https://gradle.com/terms-of-service"
    buildScan.termsOfUseAgree = "yes"
    buildScan.publishing.onlyIf {
        System.getenv("GITHUB_ACTIONS") == "true" &&
            it.buildResult.failures.isNotEmpty()
    }
}

rootProject.name = ("fr.frankois944.spm.kmp")

include(":plugin")
