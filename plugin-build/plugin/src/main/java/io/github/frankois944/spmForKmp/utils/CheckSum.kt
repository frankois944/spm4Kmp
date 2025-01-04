package io.github.frankois944.spmForKmp.utils

import java.io.File
import java.math.BigInteger
import java.security.MessageDigest

internal fun File.md5(): String {
    val data = this.readBytes()
    val hash = MessageDigest.getInstance("MD5").digest(data)
    return BigInteger(1, hash).toString(16)
}
