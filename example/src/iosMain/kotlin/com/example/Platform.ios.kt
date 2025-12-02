@file:OptIn(ExperimentalForeignApi::class)

package com.example

import DummyFramework.MyDummyFramework
import FirebaseAnalytics.FIRConsentStatusGranted
import FirebaseCore.FIRApp
import HevSocks5Tunnel.hev_socks5_tunnel_quit
import kotlinx.cinterop.ExperimentalForeignApi
import nativeIosShared.MySwiftDummyClass
import nativeIosShared.TestClass
import platform.UIKit.UIDevice
import platform.UIKit.UIView
import registrydummy.RegistryDummy

class IOSPlatform : Platform {
    fun getMyFrameworkResource(): String = MyDummyFramework().getMyResource()

    fun getMyPackageResource(): String = LocalSourceDummyFramework.LocalSourceDummy().getMyInternalResource()

    override val name: String =
        UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

fun test() {
    hev_socks5_tunnel_quit()
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
