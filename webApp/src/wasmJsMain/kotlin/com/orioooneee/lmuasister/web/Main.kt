package com.orioooneee.lmuasister.web

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.orioooneee.lmuasister.App
import com.orioooneee.lmuasister.analytics.initWebTelemetry

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    initWebTelemetry()

    ComposeViewport {
        App()
    }
}
