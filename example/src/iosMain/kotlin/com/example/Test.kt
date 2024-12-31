package com.example
import nativeExample.TestClass


@kotlinx.cinterop.ExperimentalForeignApi
val getSwiftValue = TestClass().getSomeValue()
