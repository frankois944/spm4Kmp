@file:OptIn(ExperimentalForeignApi::class)

package com.example

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString

class IOSPlatform : Platform {
    override val name: String = "Linux"
}

actual fun getPlatform(): Platform = IOSPlatform()

