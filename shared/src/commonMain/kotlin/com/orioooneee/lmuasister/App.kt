package com.orioooneee.lmuasister

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import com.orioooneee.lmuasister.config.BuildConfig
import com.orioooneee.lmuasister.data.image.SvgCssInlineDecoder
import com.orioooneee.lmuasister.data.mock.mockModule
import com.orioooneee.lmuasister.data.steam.steamModule
import com.orioooneee.lmuasister.di.appModule
import com.orioooneee.lmuasister.di.platformModules
import com.orioooneee.lmuasister.ui.MainShell
import com.orioooneee.lmuasister.ui.theme.LmuTheme
import org.koin.compose.KoinApplication
import org.koin.dsl.koinConfiguration

@Composable
fun App(startupEffects: @Composable () -> Unit = {}) {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components {
                add(KtorNetworkFetcherFactory())
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
