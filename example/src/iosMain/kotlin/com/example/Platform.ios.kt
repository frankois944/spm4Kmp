@file:OptIn(ExperimentalForeignApi::class)

package com.example

import kotlinx.cinterop.ExperimentalForeignApi
import nativeShared.MySwiftDummyClass
import platform.UIKit.UIDevice

class IOSPlatform : Platform {
    override val name: String =
        UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual fun getPlatform(): Platform = IOSPlatform()

val myNativeClass = MySwiftDummyClass().mySwiftDummyFunction()
