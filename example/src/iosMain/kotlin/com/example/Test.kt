package com.example
import FirebaseCore.FIRApp
import kotlinx.cinterop.ExperimentalForeignApi
import nativeShared.TestClass

@ExperimentalForeignApi
val getSwiftValue = TestClass().getSomeValue()

@ExperimentalForeignApi
public fun configureFirebase() = FIRApp.configure()
// val testFirebaseAnalyticsVersion = FIRAnalytics.version()
