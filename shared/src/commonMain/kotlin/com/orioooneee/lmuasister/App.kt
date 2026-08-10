package com.orioooneee.lmuasister

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.setSingletonImageLoaderFactory
import coil3.memory.MemoryCache
import coil3.network.DeDupeConcurrentRequestStrategy
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.size.Precision
import com.orioooneee.lmuasister.config.BuildConfig
import com.orioooneee.lmuasister.data.image.createImageDiskCache
import com.orioooneee.lmuasister.data.image.SvgCssInlineDecoder
import com.orioooneee.lmuasister.data.mock.mockModule
import com.orioooneee.lmuasister.data.steam.steamModule
import com.orioooneee.lmuasister.di.appModule
import com.orioooneee.lmuasister.di.platformModules
import com.orioooneee.lmuasister.network.platformHttpClient
import com.orioooneee.lmuasister.ui.MainShell
import com.orioooneee.lmuasister.ui.theme.LmuTheme
import org.koin.compose.KoinApplication
import org.koin.dsl.koinConfiguration

@Composable
@OptIn(ExperimentalCoilApi::class)
fun App(startupEffects: @Composable () -> Unit = {}) {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            // Retain constraint-sized card artwork while lazy lists recycle their composables.
            // Coil's non-Android default is only 15% of a conservative 512 MB estimate.
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.20)
                    .build()
            }
            // Reuse a larger cached bitmap for smaller cards and tolerate tiny measurement
            // differences. INEXACT never accepts a bitmap that would need to be upscaled.
            .precision(Precision.INEXACT)
            // Coil's multiplatform default cache lives in the system temporary directory.
            // Use the app's persistent cache directory so track artwork survives restarts.
            .diskCache { createImageDiskCache(context) }
            .components {
                add(
                    KtorNetworkFetcherFactory(
                        httpClient = platformHttpClient(),
                        concurrentRequestStrategy = DeDupeConcurrentRequestStrategy(),
                    ),
                )
                add(SvgCssInlineDecoder.Factory())
            }
            .build()
    }

    val steamModule = if (BuildConfig.USE_MOCK) {
        mockModule
    } else {
        steamModule()
    }

    val modules = buildList {
        add(appModule)
        add(steamModule)
        addAll(platformModules())
    }

    KoinApplication(
        configuration = koinConfiguration {
            modules(modules)
        }
    ) {
        startupEffects()
        LmuTheme {
            MainShell()
        }
    }
}

@Preview
@Composable
fun AppPreview() {
    App()
}
