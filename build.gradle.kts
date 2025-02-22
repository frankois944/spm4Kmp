import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import io.gitlab.arturbosch.detekt.Detekt
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
    alias(libs.plugins.kotlin) apply false
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.versionCheck)
    alias(libs.plugins.publish) apply false
    alias(libs.plugins.androidLibrary) apply false
}

subprojects {
    apply {
        plugin(
            rootProject.libs.plugins.detekt
                .get()
                .pluginId,
        )
        plugin(
            rootProject.libs.plugins.ktlint
                .get()
                .pluginId,
        )
    }

    ktlint {
        android.set(false)
        outputToConsole.set(false)
        ignoreFailures.set(true)
        enableExperimentalRules.set(true)
        reporters {
            reporter(ReporterType.JSON)
        }
        filter {
            include("**/kotlin/**")
            exclude("**/generated/**", "**/kotlin/functionalTest/**")
        }
    }

    detekt {
        config.setFrom(rootProject.files("config/detekt/detekt.yml"))
    }
}

tasks.withType<Detekt>().configureEach {
    reports {
        html.required.set(true)
        html.outputLocation.set(file("build/reports/detekt.html"))
    }
}

tasks.withType<DependencyUpdatesTask> {
    rejectVersionIf {
        candidate.version.isNonStable()
    }
}

fun String.isNonStable() = "^[0-9,.v-]+(-r)?$".toRegex().matches(this).not()

tasks.register("clean", Delete::class.java) {
    delete(rootProject.layout.buildDirectory)
}

tasks.register("reformatAll") {
    description = "Reformat all the Kotlin Code"

    dependsOn("ktlintFormat")
    dependsOn(gradle.includedBuild("plugin-build").task(":plugin:ktlintFormat"))
}

tasks.register("preMerge") {
    description = "Runs all the tests/verification tasks on both top level and included build."

    dependsOn(":example:check")
    dependsOn(gradle.includedBuild("plugin-build").task(":plugin:check"))
    dependsOn(gradle.includedBuild("plugin-build").task(":plugin:validatePlugins"))
}

tasks.wrapper {
    distributionType = Wrapper.DistributionType.ALL
}
