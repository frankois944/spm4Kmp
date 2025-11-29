pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
        maven("https://packages.jetbrains.team/maven/p/kt/dev")
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
        mavenCentral()
        maven("https://packages.jetbrains.team/maven/p/kt/dev")
    }

    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = ("io.github.frankois944.spmForKmp")

include(":plugin")
