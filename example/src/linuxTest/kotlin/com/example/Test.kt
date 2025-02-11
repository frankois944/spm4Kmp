@file:OptIn(BetaInteropApi::class)

package com.example

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.toKString
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
class Test {
    @Test
    fun exampleTest() {
        assertEquals("Hello from Linux Swift!", nativeLinuxShared.mySwiftDummyFunction()?.toKString())
    }
}
