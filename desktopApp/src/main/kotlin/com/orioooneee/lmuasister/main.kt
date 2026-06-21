package com.orioooneee.lmuasister

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "LMU Assister",
        icon = painterResource("icon.png"),
    ) {
        App()
    }
}