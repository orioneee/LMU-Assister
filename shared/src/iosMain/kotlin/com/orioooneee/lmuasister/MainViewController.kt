package com.orioooneee.lmuasister

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

internal object IosRootViewController {
    var current: UIViewController? = null
}

fun MainViewController(): UIViewController =
    ComposeUIViewController {
        App(startupEffects = { IosFcmTokenStartupEffect() })
    }.also { IosRootViewController.current = it }
