package com.orioooneee.lmuasister

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform