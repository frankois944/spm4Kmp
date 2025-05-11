@file:OptIn(BetaInteropApi::class)

package com.example

import DummyFramework.MyDummyFramework
import FirebaseAnalytics.FIRConsentStatusGranted
import kotlinx.cinterop.BetaInteropApi
import nativeIosShared.TestClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
class Test {
    @Test
    fun getDataFromBridgeTest() {
        assertEquals("HelloTest!", TestClass().getSomeValue())
    }

    @Test
    fun getDataFromSwiftDependencyTest() {
        assertEquals("202cb962ac59075b964b07152d234b70", TestClass().getValueFromCrypt())
    }

    @Test
    fun getLocalPackageDataTest() {
        assertEquals("TEST DUMMY FRAMEWORK", LocalSourceDummyFramework.LocalSourceDummy().test())
    }

    @Test
    fun checkNullableTest() {
        assertNotNull(FIRConsentStatusGranted, "TEST DUMMY FRAMEWORK")
    }

    /*@Test
    fun getResourceFromPackageTest() {
        assertEquals("please read my content", LocalSourceDummyFramework.LocalSourceDummy().getMyInternalResource())
    }*/

    @Test
    fun getResourceFromFrameworkTest() {
        assertEquals("please read my content\n", MyDummyFramework().getMyResource())
    }
}
