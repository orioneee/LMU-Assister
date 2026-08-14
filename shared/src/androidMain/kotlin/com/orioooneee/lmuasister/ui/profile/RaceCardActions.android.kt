package com.orioooneee.lmuasister.ui.profile

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File

@Composable
actual fun rememberRaceCardActionsController(): RaceCardActionsController {
    val context = LocalContext.current
    return remember(context) { AndroidRaceCardActionsController(context) }
}

private class AndroidRaceCardActionsController(
    private val context: Context,
) : RaceCardActionsController {
    override val canShare: Boolean = true
    override val canCopy: Boolean = true

    override suspend fun sharePng(bytes: ByteArray, fileName: String): RaceCardActionResult =
        runCatching {
            val uri = writeCard(bytes, fileName)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = ClipData.newUri(context.contentResolver, "LMU race card", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "Share race card")
            if (context !is Activity) chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            RaceCardActionResult(true, "Race card ready to share")
        }.getOrElse {
            RaceCardActionResult(false, it.message ?: "Couldn't share the race card", cause = it)
        }

    override suspend fun copyPng(bytes: ByteArray, fileName: String): RaceCardActionResult =
        runCatching {
            val uri = writeCard(bytes, fileName)
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(
                ClipData.newUri(context.contentResolver, "LMU race card", uri),
            )
            RaceCardActionResult(true, "Race card copied")
        }.getOrElse {
            RaceCardActionResult(false, it.message ?: "Couldn't copy the race card", cause = it)
        }

    private fun writeCard(bytes: ByteArray, fileName: String): android.net.Uri {
        require(bytes.isNotEmpty()) { "The race card is empty" }
        val directory = File(context.cacheDir, "shared-race-cards").apply { mkdirs() }
        val safeName = fileName.replace(Regex("[^A-Za-z0-9._-]"), "-")
        val file = File(directory, safeName).apply { writeBytes(bytes) }
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
    }
}
