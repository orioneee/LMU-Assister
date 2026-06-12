package com.orioooneee.lmuasister

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Lmu assister",
    ) {
        App()
    }
}