package com.orioooneee.lmuasister.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.orioooneee.lmuasister.isTouchPlatform

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RefreshableContent(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    topInset: Dp = 0.dp,
    content: @Composable () -> Unit,
) {
    if (isTouchPlatform) {
        val state = rememberPullToRefreshState()
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            state = state,
            modifier = Modifier.fillMaxSize(),
            // The default indicator sits at the very top and overlaps the status bar; push it
            // down by the top inset so it spins below the system bar.
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = state,
                    isRefreshing = isRefreshing,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = topInset),
                )
            },
        ) { content() }
    } else {
        content()
    }
}
