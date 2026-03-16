package io.github.frankois944.spmForKmp.utils

internal fun compareVersions(
    left: String,
    right: String,
): Int {
    val leftParts = left.split('.', '-', '_')
    val rightParts = right.split('.', '-', '_')
    val maxSize = maxOf(leftParts.size, rightParts.size)

    for (i in 0 until maxSize) {
        val l = leftParts.getOrNull(i)?.toIntOrNull()
        val r = rightParts.getOrNull(i)?.toIntOrNull()

        val result =
            when {
                l != null && r != null -> l.compareTo(r)
                else -> (leftParts.getOrNull(i) ?: "").compareTo(rightParts.getOrNull(i) ?: "")
            }

        if (result != 0) return result
    }

    return 0
}
