package io.github.frankois944.spmForKmp.fixture

import org.intellij.lang.annotations.Language

class KotlinSource private constructor(
    val packageName: String,
    val className: String,
    val imports: List<String>,
    @Language("kotlin") val content: String,
) {
    companion object {
        fun of(
            packageName: String = "com.example",
            className: String = "Test",
            imports: List<String> = emptyList(),
            @Language("kotlin") content: String = "class EmptyTest",
        ): KotlinSource = KotlinSource(packageName, className, imports, content)

        fun default() =
            of(
                content =
                    """
                    class EmptyTest
                    """.trimIndent(),
            )
    }
}
