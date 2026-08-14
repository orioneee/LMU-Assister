package com.orioooneee.lmuasister.ui.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlin.io.encoding.Base64
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

    override suspend fun sharePng(bytes: ByteArray, fileName: String): RaceCardActionResult =
        runCatching {
            when (jsString(shareRaceCardJs(Base64.Default.encode(bytes), fileName).await())) {
                "shared" -> RaceCardActionResult(true, "Race card shared")
                else -> RaceCardActionResult(true, "Race card downloaded")
            }
        }.getOrElse {
            RaceCardActionResult(false, it.message ?: "Couldn't share the race card", cause = it)
        }

    override suspend fun copyPng(bytes: ByteArray, fileName: String): RaceCardActionResult =
        runCatching {
            copyRaceCardJs(Base64.Default.encode(bytes)).await()
            RaceCardActionResult(true, "Race card copied")
        }.getOrElse {
            RaceCardActionResult(false, it.message ?: "Image clipboard isn't available in this browser", cause = it)
        }
}

@OptIn(ExperimentalWasmJsInterop::class)
private fun supportsImageClipboard(): Boolean =
    js("Boolean(globalThis.navigator?.clipboard?.write && globalThis.ClipboardItem)")

@OptIn(ExperimentalWasmJsInterop::class)
@Suppress("UNUSED_PARAMETER")
private fun shareRaceCardJs(base64: String, fileName: String): Promise<JsAny?> =
    js(
        """
        {
          const binary = globalThis.atob(base64);
          const bytes = new Uint8Array(binary.length);
          for (let i = 0; i < binary.length; i += 1) bytes[i] = binary.charCodeAt(i);
          const blob = new Blob([bytes], { type: "image/png" });
          const file = new File([blob], fileName, { type: "image/png" });
          const payload = { title: "LMU race card", files: [file] };
          if (globalThis.navigator?.share &&
              (!globalThis.navigator.canShare || globalThis.navigator.canShare(payload))) {
            return globalThis.navigator.share(payload).then(() => "shared");
          }
          const url = URL.createObjectURL(blob);
          const anchor = document.createElement("a");
          anchor.href = url;
          anchor.download = fileName;
          anchor.style.display = "none";
          document.body.appendChild(anchor);
          anchor.click();
          anchor.remove();
          setTimeout(() => URL.revokeObjectURL(url), 1000);
          return Promise.resolve("downloaded");
        }
        """,
    )

@OptIn(ExperimentalWasmJsInterop::class)
@Suppress("UNUSED_PARAMETER")
private fun copyRaceCardJs(base64: String): Promise<JsAny?> =
    js(
        """
        {
          if (!globalThis.navigator?.clipboard?.write || !globalThis.ClipboardItem) {
            return Promise.reject(new Error("Image clipboard unavailable"));
          }
          const binary = globalThis.atob(base64);
          const bytes = new Uint8Array(binary.length);
          for (let i = 0; i < binary.length; i += 1) bytes[i] = binary.charCodeAt(i);
          const blob = new Blob([bytes], { type: "image/png" });
          return globalThis.navigator.clipboard.write([
            new ClipboardItem({ "image/png": blob })
          ]).then(() => "copied");
        }
        """,
    )

@OptIn(ExperimentalWasmJsInterop::class)
private fun jsString(value: JsAny?): String = js("String(value)")
