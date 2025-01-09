package com.example
import FirebaseAnalytics.FIRConsentStatusGranted
import FirebaseCore.FIRApp
import kotlinx.cinterop.ExperimentalForeignApi
import nativeShared.TestClass

@ExperimentalForeignApi
val getSwiftValue = TestClass().getSomeValue()

@ExperimentalForeignApi
public fun configureFirebase() = FIRApp.configure()

@ExperimentalForeignApi
public val consentStatusGranted = FIRConsentStatusGranted
