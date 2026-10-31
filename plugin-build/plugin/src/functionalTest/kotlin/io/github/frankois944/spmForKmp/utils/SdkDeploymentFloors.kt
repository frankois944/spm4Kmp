package io.github.frankois944.spmForKmp.utils

/**
 * The deployment target range an installed Apple SDK accepts, read the same way the plugin reads
 * it.
 *
 * A test asserting on the deployment target check cannot hardcode a version: the floor comes from
 * the SDK, so it moves with the Xcode the build runs on. Xcode 26 accepts iOS 12, Xcode 27 does
 * not — which is exactly what this is for.
 */
object SdkDeploymentFloors {
    /** The lowest deployment target [sdk] still accepts, or `null` when it cannot be read. */
    fun lowestSupported(sdk: String): String? {
        val sdkPath = run("xcrun", "--sdk", sdk, "--show-sdk-path")?.trim().orEmpty()
        if (sdkPath.isEmpty()) return null
        val values =
            run(
                "plutil",
                "-extract",
                "DefaultProperties.DEPLOYMENT_TARGET_SUGGESTED_VALUES",
                "json",
                "-o",
                "-",
                "$sdkPath/SDKSettings.plist",
            ) ?: return null
        return VERSION
            .findAll(values)
            .map { it.groupValues[1] }
            .minWithOrNull(::compareVersions)
    }

    /**
     * A deployment target strictly below [floor], or `null` when there is none to express — the
     * check cannot then be exercised and the test has nothing to assert.
     */
    fun justBelow(floor: String): String? {
        val major = floor.substringBefore('.').toIntOrNull() ?: return null
        val minor = floor.substringAfter('.', "0").toIntOrNull() ?: 0
        return when {
            minor > 0 -> "$major.${minor - 1}"
            major > 1 -> "${major - 1}.0"
            else -> null
        }
    }

    fun compareVersions(
        left: String,
        right: String,
    ): Int {
        val leftParts = left.trim().split(".")
        val rightParts = right.trim().split(".")
        for (index in 0 until maxOf(leftParts.size, rightParts.size)) {
            val l = leftParts.getOrNull(index)?.toIntOrNull() ?: 0
            val r = rightParts.getOrNull(index)?.toIntOrNull() ?: 0
            if (l != r) return l.compareTo(r)
        }
        return 0
    }

    private val VERSION = """"(\d+(?:\.\d+)*)"""".toRegex()

    private fun run(vararg command: String): String? =
        runCatching {
            val process =
                ProcessBuilder(*command)
                    .redirectErrorStream(false)
                    .start()
            val output = process.inputStream.bufferedReader().readText()
            if (process.waitFor() == 0) output else null
        }.getOrNull()
}
