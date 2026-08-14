package com.orioooneee.lmuasister.ui.profile

import androidx.compose.runtime.Composable

data class RaceCardActionResult(
    val success: Boolean,
    val message: String,
    val cause: Throwable? = null,
)

interface RaceCardActionsController {
    val canShare: Boolean
    val canCopy: Boolean

    suspend fun sharePng(bytes: ByteArray, fileName: String): RaceCardActionResult
    suspend fun copyPng(bytes: ByteArray, fileName: String): RaceCardActionResult
}

@Composable
expect fun rememberRaceCardActionsController(): RaceCardActionsController
