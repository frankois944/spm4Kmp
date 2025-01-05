@file:OptIn(BetaInteropApi::class)

package com.example

import kotlinx.cinterop.BetaInteropApi
import nativeShared.TestClass
import kotlin.test.Test
import kotlin.test.assertEquals

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
}
