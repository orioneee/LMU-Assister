package com.orioooneee.lmuasister

class WasmPlatform : Platform {
    override val name: String = "WasmJS"
}

actual fun getPlatform(): Platform = WasmPlatform()
