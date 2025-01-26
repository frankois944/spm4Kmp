package com.example

class MacosPlatform : Platform {
    override val name: String = "MacOS"
}

actual fun getPlatform(): Platform = MacosPlatform()
