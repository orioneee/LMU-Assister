package com.orioooneee.lmuasister.data.image

import coil3.PlatformContext
import okio.Path
import okio.Path.Companion.toPath

internal actual fun imageDiskCacheDirectory(context: PlatformContext): Path? =
    System.getProperty("user.home")
        ?.takeIf { it.isNotBlank() }
        ?.toPath()
        ?.resolve(".cache")
        ?.resolve("lmu-assister")
        ?.resolve("coil_images")
