package com.orioooneee.lmuasister.data.image

import coil3.PlatformContext
import okio.Path

// Browser storage is managed by the browser's HTTP cache; Okio has no persistent file system here.
internal actual fun imageDiskCacheDirectory(context: PlatformContext): Path? = null
