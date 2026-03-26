package io.github.frankois944.spmForKmp.fixture

import org.intellij.lang.annotations.Language

class SwiftSource private constructor(
    val filename: String,
    val cinteropName: String? = null,
    @param:Language("Swift") val content: String,
) {
    companion object {
        fun of(
            filename: String = "Test.swift",
            cinteropName: String? = null,
            @Language("Swift") content: String,
        ): SwiftSource = SwiftSource(filename, cinteropName, content)
    }
}
