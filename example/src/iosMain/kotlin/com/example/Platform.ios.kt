@file:OptIn(ExperimentalForeignApi::class)

package com.example

import DummyFramework.MyDummyFramework
import FirebaseAnalytics.FIRConsentStatusGranted
import FirebaseCore.FIRApp
import kotlinx.cinterop.ExperimentalForeignApi
import nativeIosShared.MySwiftDummyClass
import nativeIosShared.TestClass
import platform.UIKit.UIDevice
import platform.UIKit.UIView

class IOSPlatform : Platform {

    init {
        println("Load data from framework" + MyDummyFramework().getMyResource())
        println("Load data from package" + LocalSourceDummyFramework.LocalSourceDummy().getMyInternalResource())
    }

    override val name: String =
        UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual fun getPlatform(): Platform = IOSPlatform()

val myNativeClass = MySwiftDummyClass().mySwiftDummyFunction()

val getSwiftValue = TestClass().getSomeValue()

fun getView() = TestClass().getView()

fun setView(view: UIView) = TestClass().setViewWithView(view)

fun getView2(): UIView = TestClass().getViewWithNSObject() as UIView

fun setView2(view: UIView) = TestClass().setViewWithNSObjectWithView(view)

fun configureFirebase() = FIRApp.configure()

val consentStatusGranted = FIRConsentStatusGranted

val localSourceDummyTest = LocalSourceDummyFramework.LocalSourceDummy().test()
