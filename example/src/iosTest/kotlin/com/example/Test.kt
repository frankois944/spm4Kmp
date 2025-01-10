@file:OptIn(BetaInteropApi::class)

package com.example

import FirebaseAnalytics.FIRConsentStatusGranted
import kotlinx.cinterop.BetaInteropApi
import nativeShared.TestClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
class Test {
    @Test
    fun exampleTest() {
        assertEquals("HelloTest!", TestClass().getSomeValue())
    }

    @Test
    fun exampleTest2() {
        assertEquals("202cb962ac59075b964b07152d234b70", TestClass().getValueFromCrypt())
    }

    @Test
    fun exampleTest3() {
        assertEquals("TEST DUMMY FRAMEWORK", LocalSourceDummyFramework.LocalSourceDummy().test())
    }

    @Test
    fun exampleTest4() {
        assertNotNull("TEST DUMMY FRAMEWORK", FIRConsentStatusGranted)
    }
}
