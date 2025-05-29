package io.github.frankois944.spmForKmp.utils

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
    fun hashDirectory(directory: File): String {
        val md5Digest = MessageDigest.getInstance("SHA256")

        @Suppress("MagicNumber")
        val buffer = ByteArray(8192) // 8KB buffer

        directory
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
