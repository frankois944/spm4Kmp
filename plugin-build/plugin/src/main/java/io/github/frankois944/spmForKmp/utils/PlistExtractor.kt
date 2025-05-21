package io.github.frankois944.spmForKmp.utils

import org.gradle.internal.impldep.com.dd.plist.NSDictionary
import org.gradle.internal.impldep.com.dd.plist.PropertyListParser
import java.io.File


internal fun getPlistValue(file: File, key: String): String? {
    try {
        val rootDict = PropertyListParser.parse(file) as NSDictionary
        return rootDict.objectForKey(key).toString()
    } catch (ex: Exception) {
        ex.printStackTrace()
    }
    return null
}
