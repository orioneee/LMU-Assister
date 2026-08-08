package com.orioooneee.lmuasister.data.image

import coil3.PlatformContext
import coil3.disk.DiskCache
import okio.Path

private const val IMAGE_CACHE_SIZE_BYTES = 150L * 1024 * 1024

internal expect fun imageDiskCacheDirectory(context: PlatformContext): Path?

internal fun createImageDiskCache(context: PlatformContext): DiskCache? =
    imageDiskCacheDirectory(context)?.let { directory ->
        DiskCache.Builder()
            .directory(directory)
            .maxSizeBytes(IMAGE_CACHE_SIZE_BYTES)
            .build()
    }
