package com.orioooneee.lmuasister

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

val isTouchPlatform: Boolean
    get() = getPlatform().name.let {
        it.startsWith("Android") || it.startsWith("iOS") || it.contains("iPhone") || it.contains("iPad")
    }

val supportsSteamGuardMobileApproval: Boolean
    get() = getPlatform().name.let {
        it.startsWith("Android") || it.startsWith("Java") || it.startsWith("iOS")
    }
