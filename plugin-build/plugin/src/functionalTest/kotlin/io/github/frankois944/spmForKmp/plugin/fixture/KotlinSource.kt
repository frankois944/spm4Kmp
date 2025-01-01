package io.github.frankois944.spmForKmp.plugin.fixture

import org.intellij.lang.annotations.Language

class KotlinSource private constructor(
    val packageName: String,
    val className: String,
    @Language("kotlin") val content: String,
) {
    companion object {
        fun of(
            packageName: String = "com.example",
            className: String = "Test",
            @Language("kotlin") content: String,
        ): KotlinSource = KotlinSource(packageName, className, content)

        fun default() =
            of(
                content =
                    """
                    package com.example
                    class Test
                    """.trimIndent(),
            )
    }
}
