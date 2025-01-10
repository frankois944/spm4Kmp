@file:OptIn(ExperimentalForeignApi::class)

package com.example

import FirebaseAnalytics.FIRConsentStatusGranted
import FirebaseCore.FIRApp
import kotlinx.cinterop.ExperimentalForeignApi
import nativeShared.MySwiftDummyClass
import nativeShared.TestClass
import platform.UIKit.UIDevice

class IOSPlatform : Platform {
    override val name: String =
        UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual fun getPlatform(): Platform = IOSPlatform()

val myNativeClass = MySwiftDummyClass().mySwiftDummyFunction()

@ExperimentalForeignApi
val getSwiftValue = TestClass().getSomeValue()

@ExperimentalForeignApi
fun configureFirebase() = FIRApp.configure()

@ExperimentalForeignApi
val consentStatusGranted = FIRConsentStatusGranted

@ExperimentalForeignApi
val localSourceDummyTest = LocalSourceDummyFramework.LocalSourceDummy().test()
