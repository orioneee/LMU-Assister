package com.orioooneee.lmuasister.data.image

import coil3.PlatformContext
import okio.Path
import okio.Path.Companion.toPath

internal actual fun imageDiskCacheDirectory(context: PlatformContext): Path? =
    context.cacheDir.resolve("coil_images").absolutePath.toPath()
