package com.orioooneee.lmuasister.ui.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.orioooneee.lmuasister.IosRootViewController
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIImage
import platform.UIKit.UIPasteboard
import platform.UIKit.UIViewController
import platform.UIKit.popoverPresentationController

@Composable
actual fun rememberRaceCardActionsController(): RaceCardActionsController =
    remember { IosRaceCardActionsController() }

@OptIn(ExperimentalForeignApi::class)
private class IosRaceCardActionsController : RaceCardActionsController {
    override val canShare: Boolean = true
    override val canCopy: Boolean = true

    override suspend fun sharePng(bytes: ByteArray, fileName: String): RaceCardActionResult =
        runCatching {
            val image = bytes.toImage() ?: error("The race card image couldn't be decoded")
            val presenter = IosRootViewController.current?.topPresenter()
                ?: error("The share sheet isn't available")
            val controller = UIActivityViewController(
                activityItems = listOf(image),
                applicationActivities = null,
            )
            controller.popoverPresentationController?.sourceView = presenter.view
            presenter.presentViewController(controller, animated = true, completion = null)
            RaceCardActionResult(true, "Race card ready to share")
        }.getOrElse {
            RaceCardActionResult(false, it.message ?: "Couldn't share the race card", cause = it)
        }

    override suspend fun copyPng(bytes: ByteArray, fileName: String): RaceCardActionResult =
        runCatching {
            val image = bytes.toImage() ?: error("The race card image couldn't be decoded")
            UIPasteboard.generalPasteboard.image = image
            RaceCardActionResult(true, "Race card copied")
        }.getOrElse {
            RaceCardActionResult(false, it.message ?: "Couldn't copy the race card", cause = it)
        }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun ByteArray.toImage(): UIImage? {
    if (isEmpty()) return null
    val data = usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = size.convert())
    }
    return UIImage(data = data)
}

private fun UIViewController.topPresenter(): UIViewController {
    var current = this
    while (current.presentedViewController != null) {
        current = current.presentedViewController!!
    }
    return current
}
