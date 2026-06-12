package com.orioooneee.lmuasister.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Hand-built vector icons so the app has zero dependency on material-icons-extended
 * (which is not on the Compose Multiplatform classpath here). All are 24x24 glyphs;
 * the actual color comes from the [androidx.compose.material3.Icon] tint.
 */
private fun icon(name: String, block: ImageVector.Builder.() -> Unit): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply(block).build()

private val white = SolidColor(Color.White)

val IconHome: ImageVector by lazy {
    icon("Home") {
        path(fill = white) {
            moveTo(12f, 3.2f)
            lineTo(21f, 11f)
            lineTo(18.6f, 11f)
            lineTo(18.6f, 20.5f)
            lineTo(14f, 20.5f)
            lineTo(14f, 14f)
            lineTo(10f, 14f)
            lineTo(10f, 20.5f)
            lineTo(5.4f, 20.5f)
            lineTo(5.4f, 11f)
            lineTo(3f, 11f)
            close()
        }
    }
}

val IconSchedule: ImageVector by lazy {
    icon("Schedule") {
        // three rounded list bars
        path(fill = white) {
            moveTo(4f, 5f); lineTo(20f, 5f); lineTo(20f, 8f); lineTo(4f, 8f); close()
            moveTo(4f, 10.5f); lineTo(20f, 10.5f); lineTo(20f, 13.5f); lineTo(4f, 13.5f); close()
            moveTo(4f, 16f); lineTo(14f, 16f); lineTo(14f, 19f); lineTo(4f, 19f); close()
        }
    }
}

val IconTools: ImageVector by lazy {
    icon("Tools") {
        // four squares (apps grid)
        path(fill = white) {
            moveTo(4f, 4f); lineTo(10.5f, 4f); lineTo(10.5f, 10.5f); lineTo(4f, 10.5f); close()
            moveTo(13.5f, 4f); lineTo(20f, 4f); lineTo(20f, 10.5f); lineTo(13.5f, 10.5f); close()
            moveTo(4f, 13.5f); lineTo(10.5f, 13.5f); lineTo(10.5f, 20f); lineTo(4f, 20f); close()
            moveTo(13.5f, 13.5f); lineTo(20f, 13.5f); lineTo(20f, 20f); lineTo(13.5f, 20f); close()
        }
    }
}

val IconChevronRight: ImageVector by lazy {
    icon("ChevronRight") {
        path(fill = white) {
            moveTo(9f, 6f)
            lineTo(15f, 12f)
            lineTo(9f, 18f)
            lineTo(7.4f, 16.4f)
            lineTo(11.8f, 12f)
            lineTo(7.4f, 7.6f)
            close()
        }
    }
}

val IconFlag: ImageVector by lazy {
    icon("Flag") {
        path(fill = white) {
            // pole
            moveTo(5f, 3f); lineTo(6.6f, 3f); lineTo(6.6f, 21f); lineTo(5f, 21f); close()
            // flag body
            moveTo(6.6f, 4f); lineTo(19f, 4f); lineTo(19f, 13f); lineTo(6.6f, 13f); close()
        }
    }
}

val IconBolt: ImageVector by lazy {
    icon("Bolt") {
        path(fill = white) {
            moveTo(13f, 2f)
            lineTo(5f, 13.2f)
            lineTo(11f, 13.2f)
            lineTo(9.4f, 22f)
            lineTo(19f, 10f)
            lineTo(12.3f, 10f)
            close()
        }
    }
}

val IconChampionship: ImageVector by lazy {
    icon("Trophy") {
        path(fill = white) {
            // cup bowl
            moveTo(7f, 4f); lineTo(17f, 4f); lineTo(17f, 8f)
            arcToRelative(5f, 5f, 0f, false, true, -10f, 0f); close()
            // stem + base
            moveTo(11f, 12.5f); lineTo(13f, 12.5f); lineTo(13f, 17f); lineTo(11f, 17f); close()
            moveTo(8f, 18f); lineTo(16f, 18f); lineTo(16f, 20.5f); lineTo(8f, 20.5f); close()
        }
    }
}
