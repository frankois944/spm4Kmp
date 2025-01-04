@file:OptIn(BetaInteropApi::class)

package com.example

import FirebaseCore.FIRApp
import kotlinx.cinterop.BetaInteropApi
import nativeShared.TestClass
import platform.Foundation.version
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
class Test {

    @BeforeTest
    fun setup() {
    }

    @Test
    fun exampleTest() {
        assertEquals("HelloTest!", TestClass().getSomeValue())
    }
}
