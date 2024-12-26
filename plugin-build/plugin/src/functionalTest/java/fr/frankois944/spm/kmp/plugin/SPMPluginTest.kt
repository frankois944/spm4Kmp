package fr.frankois944.spm.kmp.plugin

import com.autonomousapps.kit.AbstractGradleProject
import com.autonomousapps.kit.GradleBuilder.build
import com.autonomousapps.kit.GradleProject
import com.autonomousapps.kit.Source
import com.autonomousapps.kit.gradle.Dependency
import com.autonomousapps.kit.gradle.Plugin
import org.junit.jupiter.api.Test

class MyFixture : AbstractGradleProject() {
    // Injected into functionalTest JVM by the plugin
    // Also available via AbstractGradleProject.PLUGIN_UNDER_TEST_VERSION
    private val pluginVersion = System.getProperty("com.autonomousapps.plugin-under-test.version")

    val gradleProject: GradleProject = build()

    private fun build(): GradleProject =
        newGradleProjectBuilder()
            .withSubproject("project") {
                sources = source.toMutableList()
                withBuildScript {
                    plugins(Plugin.javaLibrary, Plugin("my-cool-plugin", pluginVersion))
                    dependencies(Dependency.implementation("com.company:library:1.0"))
                }
            }.write()

    private val source =
        listOf(
            Source
                .java(
                    """
      package com.example.project;

      public class Project {
        // do stuff here
      }
      """,
                ).withPath(packagePath = "com.example.project", className = "Project")
                .build(),
        )
}

class Test {
    @Test fun test() {
        // Given
        val project = MyFixture().gradleProject

        // When
        val result = build(project.rootDir, ":project:myTask")

        // Then
        // assertThat(result).task(":project:myTask").succeeded()
    }
}
