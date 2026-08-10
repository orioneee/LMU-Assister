package com.orioooneee.lmuasister.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.orioooneee.lmuasister.ui.IconFlag
import com.orioooneee.lmuasister.ui.theme.Surface2
import com.orioooneee.lmuasister.ui.theme.TextLow
import kotlinx.coroutines.delay
import lmuassister.shared.generated.resources.Res
import lmuassister.shared.generated.resources.couldnt_load
import org.jetbrains.compose.resources.stringResource

private enum class CoverLoadState {
    LOADING,
    SUCCESS,
    ERROR,
}

// Lazy containers dispose off-screen composables. Remember which URLs have already completed so
// re-entering composition does not flash a loader while Coil performs its asynchronous cache hit.
private val loadedCoverUrls = mutableSetOf<String>()

@Composable
fun CoverImage(
    url: String?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Crop,
    fallbackUrl: String? = null,
) {
    val primary = url?.takeIf { it.isNotBlank() }
    val fallback = fallbackUrl?.takeIf { it.isNotBlank() && it != primary }
    // Keep a successful fallback when a lazy-list item temporarily leaves composition. Otherwise
    // every scroll back to an item with a broken primary URL retries that URL before the fallback.
    var activeUrl by rememberSaveable(primary, fallback) { mutableStateOf(primary ?: fallback) }

    if (activeUrl == null) {
        Box(modifier.then(Modifier.fillMaxSize()).background(Surface2), contentAlignment = Alignment.Center) {
            Icon(IconFlag, contentDescription = null, tint = TextLow.copy(alpha = 0.35f), modifier = Modifier.size(40.dp))
        }
        return
    }

    var loadState by remember(activeUrl) { mutableStateOf(CoverLoadState.LOADING) }
    var loadedBefore by remember(activeUrl) { mutableStateOf(activeUrl in loadedCoverUrls) }
    var showLoadingOverlay by remember(activeUrl) { mutableStateOf(false) }

    // Give memory/disk cache hits time to resolve without flashing a network-style loader. Once a
    // URL has rendered successfully in this process, never show its loader again on list recycling.
    LaunchedEffect(activeUrl, loadState, loadedBefore) {
        showLoadingOverlay = false
        if (loadState == CoverLoadState.LOADING && !loadedBefore) {
            delay(250)
            if (loadState == CoverLoadState.LOADING && !loadedBefore) showLoadingOverlay = true
        }
    }

    Box(
        modifier.then(Modifier.fillMaxSize()).background(Surface2),
        contentAlignment = Alignment.Center,
    ) {
        // AsyncImage resolves the request size from these layout constraints. The previous
        // rememberAsyncImagePainter request decoded covers at their original size, quickly
        // evicting them from Coil's memory cache while scrolling.
        AsyncImage(
            model = activeUrl,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = Modifier.fillMaxSize(),
            onLoading = { loadState = CoverLoadState.LOADING },
            onSuccess = {
                activeUrl?.let(loadedCoverUrls::add)
                loadedBefore = true
                loadState = CoverLoadState.SUCCESS
            },
            onError = {
                if (activeUrl == primary && fallback != null) {
                    activeUrl = fallback
                } else {
                    loadState = CoverLoadState.ERROR
                }
            },
        )
        when {
            showLoadingOverlay -> Box(Modifier.fillMaxSize().background(shimmerBrush()))

            loadState == CoverLoadState.ERROR -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("!", style = MaterialTheme.typography.titleMedium, color = TextLow)
                Spacer(Modifier.height(2.dp))
                Text(
                    stringResource(Res.string.couldnt_load),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextLow,
                    textAlign = TextAlign.Center,
                )
            }

            else -> {}
        }
    }
}
