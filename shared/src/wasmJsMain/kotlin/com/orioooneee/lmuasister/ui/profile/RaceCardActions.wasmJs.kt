package com.orioooneee.lmuasister.ui.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.Promise
import kotlin.js.js
import kotlinx.coroutines.await

@Composable
actual fun rememberRaceCardActionsController(): RaceCardActionsController =
    remember { WebRaceCardActionsController() }

@OptIn(ExperimentalWasmJsInterop::class)
private class WebRaceCardActionsController : RaceCardActionsController {
    override val canShare: Boolean = true
    override val canCopy: Boolean = supportsImageClipboard()

    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun sharePng(bytes: ByteArray, fileName: String): RaceCardActionResult =
        runCatching {
            when (jsString(shareRaceCardJs(Base64.encode(bytes), fileName).await())) {
                "shared" -> RaceCardActionResult(true, "Race card shared")
                else -> RaceCardActionResult(true, "Race card downloaded")
            }
        }.getOrElse {
            RaceCardActionResult(false, "Couldn't share the race card", cause = it)
        }

    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun copyPng(bytes: ByteArray, fileName: String): RaceCardActionResult =
        runCatching {
            copyPngToClipboard(Base64.encode(bytes)).await<JsAny?>()
            RaceCardActionResult(true, "Race card copied")
        }.getOrElse {
            RaceCardActionResult(false, "Couldn't copy the race card", cause = it)
        }
}

@OptIn(ExperimentalWasmJsInterop::class)
private fun supportsImageClipboard(): Boolean =
    js("Boolean(globalThis.navigator?.clipboard?.write && globalThis.ClipboardItem)")

@OptIn(ExperimentalWasmJsInterop::class)
@Suppress("UNUSED_PARAMETER")
private fun shareRaceCardJs(base64: String, fileName: String): Promise<JsAny?> = js(
    """
    {
      const binary = globalThis.atob(base64);
      const bytes = new Uint8Array(binary.length);
      for (let i = 0; i < binary.length; i += 1) bytes[i] = binary.charCodeAt(i);
      const blob = new Blob([bytes], { type: 'image/png' });
      const file = new File([blob], fileName, { type: 'image/png' });
      const payload = { title: 'LMU race card', files: [file] };
      if (globalThis.navigator?.share &&
          (!globalThis.navigator.canShare || globalThis.navigator.canShare(payload))) {
        return globalThis.navigator.share(payload).then(() => 'shared');
      }
      const url = URL.createObjectURL(blob);
      const anchor = document.createElement('a');
      anchor.href = url;
      anchor.download = fileName;
      anchor.style.display = 'none';
      document.body.appendChild(anchor);
      anchor.click();
      anchor.remove();
      setTimeout(() => URL.revokeObjectURL(url), 1000);
      return Promise.resolve('downloaded');
    }
    """,
)

@OptIn(ExperimentalWasmJsInterop::class)
@Suppress("UNUSED_PARAMETER")
private fun copyPngToClipboard(base64: String): Promise<JsAny?> = js(
    """
    fetch('data:image/png;base64,' + base64)
      .then(response => response.blob())
      .then(blob => navigator.clipboard.write([new ClipboardItem({'image/png': blob})]))
    """,
)

@OptIn(ExperimentalWasmJsInterop::class)
private fun jsString(value: JsAny?): String = js("String(value)")
