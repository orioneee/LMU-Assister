package com.orioooneee.lmuasister.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage

/**
 * Cover image that fills its box by cropping. Every box we use is square or
 * wider, so cropping never zooms in tighter than a square ("up to square, no more").
 */
@Composable
fun CoverImage(
    url: String?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    AsyncImage(
        model = url,
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = modifier.then(Modifier.fillMaxSize()),
    )
}
