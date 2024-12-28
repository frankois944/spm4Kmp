package fr.frankois944.spm.kmp.plugin.fixture

import org.intellij.lang.annotations.Language

class SwiftSource private constructor(
    val filename: String,
    @Language("Swift") val content: String,
) {
    companion object {
        fun of(
            filename: String = "Test.swift",
            @Language("Swift") content: String,
        ): SwiftSource = SwiftSource(filename, content)
    }
}
