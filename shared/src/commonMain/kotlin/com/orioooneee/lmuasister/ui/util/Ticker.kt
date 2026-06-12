package com.orioooneee.lmuasister.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlin.time.Clock
import kotlin.time.Instant

/** A "now" that updates every [periodMs] so countdowns tick live. */
@Composable
fun rememberNow(periodMs: Long = 1000L): Instant {
    var now by remember { mutableStateOf(Clock.System.now()) }
    LaunchedEffect(periodMs) {
        while (true) {
            delay(periodMs)
            now = Clock.System.now()
        }
    }
    return now
}
