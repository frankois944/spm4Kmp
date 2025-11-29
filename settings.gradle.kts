pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        maven("https://packages.jetbrains.team/maven/p/kt/dev")
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
        maven("https://packages.jetbrains.team/maven/p/kt/dev")
    }
}

rootProject.name = "spmForKmp-plugin"

include(":example")
includeBuild("plugin-build")
