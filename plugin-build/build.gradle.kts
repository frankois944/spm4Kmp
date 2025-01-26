import io.gitlab.arturbosch.detekt.Detekt
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
    alias(libs.plugins.kotlin) apply false
    alias(libs.plugins.pluginPublish) apply false
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.versionCheck)
    alias(libs.plugins.autonomousapps.testkit) apply false
    alias(libs.plugins.publish) apply false
}

allprojects {
    group = property("GROUP").toString()
    version = property("VERSION").toString()

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
            exclude("**/generated/**")
            include("**/kotlin/**")
        }
    }

    detekt {
        config.setFrom(rootProject.files("../config/detekt/detekt.yml"))
    }
}

tasks.withType<Detekt>().configureEach {
    reports {
        html.required.set(true)
        html.outputLocation.set(file("build/reports/detekt.html"))
    }
}

tasks.register("clean", Delete::class.java) {
    delete(rootProject.layout.buildDirectory)
}

tasks.wrapper {
    distributionType = Wrapper.DistributionType.ALL
}
