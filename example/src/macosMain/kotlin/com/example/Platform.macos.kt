package com.example

class MacosPlatform : com.example.Platform {
    override val name: String = "MacOS"
}

actual fun getPlatform(): Platform = MacosPlatform()
