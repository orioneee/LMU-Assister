package com.orioooneee.lmuasister.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

/**
 * Cover image that never letterboxes awkwardly:
 *  - in a square-ish box it crops to fill (cropping a wide cover down to a square
 *    is fine — "up to square, no more");
 *  - in a wider box it shows the whole image (`Fit`) over a blurred, cropped copy
 *    of itself, so the sides are filled by a blur instead of black bars.
 */
@Composable
fun CoverImage(
    url: String?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    blurRadius: Int = 18,
) {
    BoxWithConstraints(modifier) {
        val ratio = if (maxHeight.value > 0f) maxWidth.value / maxHeight.value else 1f
        val squareish = ratio <= 1.15f

        if (!squareish) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize().blur(blurRadius.dp),
            )
        }
        AsyncImage(
            model = url,
            contentDescription = contentDescription,
            contentScale = if (squareish) ContentScale.Crop else ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
