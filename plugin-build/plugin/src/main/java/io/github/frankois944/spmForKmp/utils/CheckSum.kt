package io.github.frankois944.spmForKmp.utils

import io.github.frankois944.spmForKmp.config.PackageDirectoriesConfig
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.math.BigInteger
import java.security.MessageDigest

internal fun File.checkSum(): String {
    val data = this.readBytes()
    val hash = MessageDigest.getInstance("SHA256").digest(data)
    @Suppress("MagicNumber")
    return BigInteger(1, hash).toString(16)
}

internal object Hashing {
    @Throws(IOException::class)
    fun hashDirectory(config: PackageDirectoriesConfig): String {
        val md5Digest = MessageDigest.getInstance("SHA256")

        md5Digest.update(config.bridgeSourceDir.path.toByteArray())
        md5Digest.update(config.spmWorkingDir.path.toByteArray())
        md5Digest.update(config.packageScratchDir.path.toByteArray())
        config.sharedCacheDir?.path?.let {
            md5Digest.update(it.toByteArray())
        }
        config.sharedSecurityDir?.path?.let {
            md5Digest.update(it.toByteArray())
        }
        config.sharedConfigDir?.path?.let {
            md5Digest.update(it.toByteArray())
        }

        @Suppress("MagicNumber")
        val buffer = ByteArray(8192) // 8KB buffer
        config
            .bridgeSourceDir
            .walk()
            .filter { it.isFile }
            .filter { !it.isHidden }
            .sortedBy { it }
            .forEach { filePath ->
                FileInputStream(filePath).use { fis ->
                    var bytesRead: Int
                    while (fis.read(buffer).also { bytesRead = it } != -1) {
                        md5Digest.update(buffer, 0, bytesRead)
                    }
                }
            }
        return md5Digest.digest().joinToString("") { "%02x".format(it) }
    }
}
