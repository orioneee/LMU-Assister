package com.orioooneee.lmuasister

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import com.orioooneee.lmuasister.data.image.SvgCssInlineDecoder
import com.orioooneee.lmuasister.data.steam.steamModule
import com.orioooneee.lmuasister.di.appModule
import com.orioooneee.lmuasister.ui.MainShell
import com.orioooneee.lmuasister.ui.theme.LmuTheme
import org.koin.compose.KoinApplication
import org.koin.dsl.koinConfiguration

@Composable
@Preview
fun App() {
    // Coil 3 doesn't auto-register a network fetcher on non-JVM targets, so wire
    // the Ktor one in explicitly — this makes AsyncImage able to load remote images.
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components {
                add(KtorNetworkFetcherFactory())
                // SVG decoder that first inlines <style> CSS colours (Illustrator logos),
                // then delegates to Coil's real SvgDecoder. Caching stays URL-keyed.
                add(SvgCssInlineDecoder.Factory())
            }
            .build()
    }

    KoinApplication(configuration = koinConfiguration { modules(appModule, steamModule()) }) {
        LmuTheme {
            MainShell()
        }
    }
}
