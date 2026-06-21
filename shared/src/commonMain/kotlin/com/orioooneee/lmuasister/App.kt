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
import com.orioooneee.lmuasister.ui.MainShell
import com.orioooneee.lmuasister.ui.theme.LmuTheme
import org.koin.compose.KoinApplication
import org.koin.dsl.koinConfiguration

@Composable
@Preview
fun App() {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components {
                add(KtorNetworkFetcherFactory())
                add(SvgCssInlineDecoder.Factory())
            }
            .build()
    }

    // Mock data layer (no real backend) is the default for git checkouts — see BuildConfig.USE_MOCK.
    val steam = if (BuildConfig.USE_MOCK) mockModule else steamModule()
    KoinApplication(configuration = koinConfiguration { modules(appModule, steam) }) {
        LmuTheme {
            MainShell()
        }
    }
}
