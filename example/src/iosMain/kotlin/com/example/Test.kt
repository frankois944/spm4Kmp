@file:OptIn(ExperimentalForeignApi::class)

package com.example
import FirebaseAnalytics.FIRAnalytics
import FirebaseCore.FIRApp
import kotlinx.cinterop.ExperimentalForeignApi
import nativeExample.TestClass
import platform.Foundation.version

@ExperimentalForeignApi
val getSwiftValue = TestClass().getSomeValue()

val testFirebaseCoreVerion = FIRApp.version()
val testFirebaseAnalyticsVersion = FIRAnalytics.version()
