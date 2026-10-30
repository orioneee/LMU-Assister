package com.orioooneee.lmuasister.ui.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.awt.GraphicsEnvironment
import java.awt.Image
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

@Composable
actual fun rememberRaceCardActionsController(): RaceCardActionsController =
    remember { JvmRaceCardActionsController() }

private class JvmRaceCardActionsController : RaceCardActionsController {
    override val canShare: Boolean = false
    override val canCopy: Boolean = !GraphicsEnvironment.isHeadless()

    override suspend fun sharePng(bytes: ByteArray, fileName: String): RaceCardActionResult =
        RaceCardActionResult(false, "System sharing is not available on desktop")

    override suspend fun copyPng(bytes: ByteArray, fileName: String): RaceCardActionResult =
        runCatching {
            val image = ImageIO.read(ByteArrayInputStream(bytes))
                ?: error("share_card_invalid_png")
            Toolkit.getDefaultToolkit().systemClipboard.setContents(ImageTransferable(image), null)
            RaceCardActionResult(true, "Race card copied")
        }.getOrElse {
            RaceCardActionResult(false, "Couldn't copy the race card", cause = it)
        }
}

private class ImageTransferable(
    private val image: Image,
) : Transferable {
    override fun getTransferDataFlavors(): Array<DataFlavor> = arrayOf(DataFlavor.imageFlavor)

    override fun isDataFlavorSupported(flavor: DataFlavor): Boolean = flavor == DataFlavor.imageFlavor

    override fun getTransferData(flavor: DataFlavor): Any {
        check(isDataFlavorSupported(flavor)) { "Unsupported clipboard data flavor" }
        return image
    }
}
