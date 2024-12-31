package com.example

import kotlin.test.Test
import nativeExample.TestClass
import kotlin.test.assertEquals

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
class Test {
    @Test
    fun exampleTest() {
        assertEquals("HelloTest!", TestClass().getSomeValue())
    }
}
