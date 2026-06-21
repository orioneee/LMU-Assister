package com.orioooneee.lmuasister.ui.components

import androidx.compose.foundation.Image
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import com.orioooneee.lmuasister.ui.IconFlag
import com.orioooneee.lmuasister.ui.theme.Surface2
import com.orioooneee.lmuasister.ui.theme.TextLow
import lmuassister.shared.generated.resources.Res
import lmuassister.shared.generated.resources.couldnt_load
import org.jetbrains.compose.resources.stringResource

@Composable
fun CoverImage(
    url: String?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Crop,
) {
    if (url.isNullOrBlank()) {
        Box(modifier.then(Modifier.fillMaxSize()).background(Surface2), contentAlignment = Alignment.Center) {
            Icon(IconFlag, contentDescription = null, tint = TextLow.copy(alpha = 0.35f), modifier = Modifier.size(40.dp))
        }
        return
    }

    val painter = rememberAsyncImagePainter(model = url, contentScale = contentScale)
    val state by painter.state.collectAsState()

    Box(
        modifier.then(Modifier.fillMaxSize()).background(Surface2),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painter,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = Modifier.fillMaxSize(),
        )
        when (state) {
            is AsyncImagePainter.State.Loading ->
                Box(Modifier.fillMaxSize().background(shimmerBrush()))

            is AsyncImagePainter.State.Error ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⚠️", style = MaterialTheme.typography.titleMedium)
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
