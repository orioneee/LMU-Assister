package com.orioooneee.lmuasister.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
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

/** Builds a 24x24 icon straight from an SVG path string (for real brand logos). */
private fun svgIcon(name: String, pathData: String): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        addPath(PathParser().parsePathString(pathData).toNodes(), fill = white)
    }.build()

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

/** Outlined calendar — bottom-nav Schedule tab. */
val IconCalendarOutline: ImageVector by lazy {
    icon("CalendarOutline") {
        // body
        path(stroke = white, strokeLineWidth = 1.8f, strokeLineJoin = StrokeJoin.Round, strokeLineCap = StrokeCap.Round) {
            moveTo(4f, 6f); lineTo(20f, 6f); lineTo(20f, 20f); lineTo(4f, 20f); close()
        }
        // header divider
        path(stroke = white, strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round) {
            moveTo(4f, 9.5f); lineTo(20f, 9.5f)
        }
        // hanger legs
        path(stroke = white, strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round) {
            moveTo(8f, 4f); lineTo(8f, 7f)
            moveTo(16f, 4f); lineTo(16f, 7f)
        }
    }
}

/** Outlined person — bottom-nav Profile tab. */
val IconPersonOutline: ImageVector by lazy {
    icon("PersonOutline") {
        // head
        path(stroke = white, strokeLineWidth = 1.8f, strokeLineJoin = StrokeJoin.Round) {
            moveTo(15.2f, 8f)
            arcToRelative(3.2f, 3.2f, 0f, true, true, -6.4f, 0f)
            arcToRelative(3.2f, 3.2f, 0f, true, true, 6.4f, 0f)
            close()
        }
        // shoulders (open arc)
        path(stroke = white, strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round) {
            moveTo(5.5f, 19.5f)
            arcToRelative(6.5f, 6.5f, 0f, false, true, 13f, 0f)
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

val IconPerson: ImageVector by lazy {
    icon("Person") {
        path(fill = white) {
            // head
            moveTo(8f, 7.5f)
            arcToRelative(4f, 4f, 0f, true, true, 8f, 0f)
            arcToRelative(4f, 4f, 0f, true, true, -8f, 0f)
            close()
            // shoulders
            moveTo(5f, 20.5f)
            arcToRelative(7f, 7f, 0f, true, true, 14f, 0f)
            close()
        }
    }
}

// Official Steam wordmark glyph (simple-icons path), parsed from SVG.
val IconSteam: ImageVector by lazy {
    svgIcon(
        "Steam",
        "M11.979 0C5.678 0 .511 4.86.022 11.037l6.432 2.658c.545-.371 1.203-.59 " +
            "1.912-.59.063 0 .125.004.188.006l2.861-4.142V8.91c0-2.495 2.028-4.524 " +
            "4.524-4.524 2.494 0 4.524 2.031 4.524 4.527s-2.03 4.525-4.524 4.525h-.105l-4.076 " +
            "2.911c0 .052.004.105.004.159 0 1.875-1.515 3.396-3.39 3.396-1.635 " +
            "0-3.016-1.173-3.331-2.727L.436 15.27C1.862 20.307 6.486 24 11.979 24c6.627 0 " +
            "11.999-5.373 11.999-12S18.605 0 11.979 0zM7.54 18.21l-1.473-.61c.262.543.714.999 " +
            "1.314 1.25 1.297.539 2.793-.076 3.332-1.375.263-.63.264-1.319.005-1.949s-.75-1.121-1.377-1.383c-.624-.26-1.29-.249-1.878-.03l1.523.63c.956.4 " +
            "1.409 1.5 1.009 2.455-.397.957-1.497 1.41-2.454 1.012H7.54zm11.415-9.303c0-1.662-1.353-3.015-3.015-3.015-1.665 " +
            "0-3.015 1.353-3.015 3.015 0 1.665 1.35 3.015 3.015 3.015 1.663 0 3.015-1.35 " +
            "3.015-3.015zm-5.273-.005c0-1.252 1.013-2.266 2.265-2.266 1.249 0 2.266 1.014 " +
            "2.266 2.266 0 1.251-1.017 2.265-2.266 2.265-1.252 0-2.265-1.014-2.265-2.265z",
    )
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
