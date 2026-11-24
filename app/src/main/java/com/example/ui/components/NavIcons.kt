package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Modern Lucide-inspired Duotone Rounded vector icons for the bottom navigation bar.
 * Uses Image() instead of Icon() so multi-tone duotone fills and crisp rounded strokes
 * render cleanly in both Light and Dark themes without monochrome flattening.
 */
@Composable
fun NavLayoutGridIcon(
    active: Boolean,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 23.dp
) {
    val vector = remember(active, tint) {
        val stroke = SolidColor(tint)
        val fill = if (active) SolidColor(tint.copy(alpha = 0.22f)) else null
        val sw = if (active) 2.1f else 1.75f

        ImageVector.Builder(
            name = "LucideLayoutGrid",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Top-left rounded square (3..10, 3..10)
            path(
                fill = fill,
                stroke = stroke,
                strokeLineWidth = sw,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(5f, 3f)
                horizontalLineTo(8.5f)
                quadTo(10.5f, 3f, 10.5f, 5f)
                verticalLineTo(8.5f)
                quadTo(10.5f, 10.5f, 8.5f, 10.5f)
                horizontalLineTo(5f)
                quadTo(3f, 10.5f, 3f, 8.5f)
                verticalLineTo(5f)
                quadTo(3f, 3f, 5f, 3f)
                close()
            }
            // Top-right rounded square (13.5..21, 3..10.5)
            path(
                fill = fill,
                stroke = stroke,
                strokeLineWidth = sw,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(15.5f, 3f)
                horizontalLineTo(19f)
                quadTo(21f, 3f, 21f, 5f)
                verticalLineTo(8.5f)
                quadTo(21f, 10.5f, 19f, 10.5f)
                horizontalLineTo(15.5f)
                quadTo(13.5f, 10.5f, 13.5f, 8.5f)
                verticalLineTo(5f)
                quadTo(13.5f, 3f, 15.5f, 3f)
                close()
            }
            // Bottom-right rounded square (13.5..21, 13.5..21)
            path(
                fill = fill,
                stroke = stroke,
                strokeLineWidth = sw,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(15.5f, 13.5f)
                horizontalLineTo(19f)
                quadTo(21f, 13.5f, 21f, 15.5f)
                verticalLineTo(19f)
                quadTo(21f, 21f, 19f, 21f)
                horizontalLineTo(15.5f)
                quadTo(13.5f, 21f, 13.5f, 19f)
                verticalLineTo(15.5f)
                quadTo(13.5f, 13.5f, 15.5f, 13.5f)
                close()
            }
            // Bottom-left rounded square (3..10.5, 13.5..21)
            path(
                fill = fill,
                stroke = stroke,
                strokeLineWidth = sw,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(5f, 13.5f)
                horizontalLineTo(8.5f)
                quadTo(10.5f, 13.5f, 10.5f, 15.5f)
                verticalLineTo(19f)
                quadTo(10.5f, 21f, 8.5f, 21f)
                horizontalLineTo(5f)
                quadTo(3f, 21f, 3f, 19f)
                verticalLineTo(15.5f)
                quadTo(3f, 13.5f, 5f, 13.5f)
                close()
            }
        }.build()
    }

    Image(
        imageVector = vector,
        contentDescription = null,
        modifier = modifier.size(size)
    )
}

@Composable
fun NavCircleArrowDownIcon(
    active: Boolean,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 23.dp
) {
    val vector = remember(active, tint) {
        val stroke = SolidColor(tint)
        val fill = if (active) SolidColor(tint.copy(alpha = 0.20f)) else null
        val sw = if (active) 2.15f else 1.75f

        ImageVector.Builder(
            name = "LucideCircleArrowDown",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Outer circle (cx=12, cy=12, r=9.5)
            path(
                fill = fill,
                stroke = stroke,
                strokeLineWidth = sw,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 2.5f)
                arcTo(9.5f, 9.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, 11.99f, 2.5f)
                close()
            }
            // Vertical shaft
            path(
                stroke = stroke,
                strokeLineWidth = sw,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 8f)
                verticalLineTo(16f)
            }
            // Down chevron
            path(
                stroke = stroke,
                strokeLineWidth = sw,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(8.5f, 12.5f)
                lineTo(12f, 16f)
                lineTo(15.5f, 12.5f)
            }
        }.build()
    }

    Image(
        imageVector = vector,
        contentDescription = null,
        modifier = modifier.size(size)
    )
}

@Composable
fun NavCircleArrowUpIcon(
    active: Boolean,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 23.dp
) {
    val vector = remember(active, tint) {
        val stroke = SolidColor(tint)
        val fill = if (active) SolidColor(tint.copy(alpha = 0.20f)) else null
        val sw = if (active) 2.15f else 1.75f

        ImageVector.Builder(
            name = "LucideCircleArrowUp",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Outer circle (cx=12, cy=12, r=9.5)
            path(
                fill = fill,
                stroke = stroke,
                strokeLineWidth = sw,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 2.5f)
                arcTo(9.5f, 9.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, 11.99f, 2.5f)
                close()
            }
            // Vertical shaft
            path(
                stroke = stroke,
                strokeLineWidth = sw,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 16f)
                verticalLineTo(8f)
            }
            // Up chevron
            path(
                stroke = stroke,
                strokeLineWidth = sw,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(8.5f, 11.5f)
                lineTo(12f, 8f)
                lineTo(15.5f, 11.5f)
            }
        }.build()
    }

    Image(
        imageVector = vector,
        contentDescription = null,
        modifier = modifier.size(size)
    )
}

@Composable
fun NavReceiptTextIcon(
    active: Boolean,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 23.dp
) {
    val vector = remember(active, tint) {
        val stroke = SolidColor(tint)
        val fill = if (active) SolidColor(tint.copy(alpha = 0.20f)) else null
        val sw = if (active) 2.1f else 1.75f

        ImageVector.Builder(
            name = "LucideReceiptText",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Crisp Lucide receipt silhouette with soft scalloped bottom
            path(
                fill = fill,
                stroke = stroke,
                strokeLineWidth = sw,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(4.5f, 4.5f)
                quadTo(4.5f, 2.5f, 6.5f, 2.5f)
                horizontalLineTo(17.5f)
                quadTo(19.5f, 2.5f, 19.5f, 4.5f)
                verticalLineTo(21f)
                lineTo(17f, 19.3f)
                lineTo(14.5f, 21f)
                lineTo(12f, 19.3f)
                lineTo(9.5f, 21f)
                lineTo(7f, 19.3f)
                lineTo(4.5f, 21f)
                close()
            }
            // Receipt text lines
            path(
                stroke = stroke,
                strokeLineWidth = sw,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(8.5f, 7.5f)
                horizontalLineTo(15.5f)
                moveTo(8.5f, 11.5f)
                horizontalLineTo(15.5f)
                moveTo(8.5f, 15.5f)
                horizontalLineTo(12.5f)
            }
        }.build()
    }

    Image(
        imageVector = vector,
        contentDescription = null,
        modifier = modifier.size(size)
    )
}

@Composable
fun NavBarChartIcon(
    active: Boolean,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 23.dp
) {
    val vector = remember(active, tint) {
        val stroke = SolidColor(tint)
        val fill = if (active) SolidColor(tint.copy(alpha = 0.24f)) else null
        val sw = if (active) 2.1f else 1.75f

        ImageVector.Builder(
            name = "LucideBarChart3",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Rounded L-axis frame
            path(
                stroke = stroke,
                strokeLineWidth = sw,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(3.5f, 3.5f)
                verticalLineTo(18.5f)
                quadTo(3.5f, 20.5f, 5.5f, 20.5f)
                horizontalLineTo(20.5f)
            }
            // Bar 1 (medium-short rounded capsule)
            path(
                fill = fill,
                stroke = stroke,
                strokeLineWidth = sw,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(7.5f, 12.2f)
                quadTo(7.5f, 11f, 8.7f, 11f)
                horizontalLineTo(9.3f)
                quadTo(10.5f, 11f, 10.5f, 12.2f)
                verticalLineTo(17.5f)
                horizontalLineTo(7.5f)
                close()
            }
            // Bar 2 (tall rounded capsule)
            path(
                fill = fill,
                stroke = stroke,
                strokeLineWidth = sw,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12.2f, 6.2f)
                quadTo(12.2f, 5f, 13.4f, 5f)
                horizontalLineTo(14f)
                quadTo(15.2f, 5f, 15.2f, 6.2f)
                verticalLineTo(17.5f)
                horizontalLineTo(12.2f)
                close()
            }
            // Bar 3 (medium rounded capsule)
            path(
                fill = fill,
                stroke = stroke,
                strokeLineWidth = sw,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(16.9f, 9.5f)
                quadTo(16.9f, 8.3f, 18.1f, 8.3f)
                horizontalLineTo(18.7f)
                quadTo(19.9f, 8.3f, 19.9f, 9.5f)
                verticalLineTo(17.5f)
                horizontalLineTo(16.9f)
                close()
            }
        }.build()
    }

    Image(
        imageVector = vector,
        contentDescription = null,
        modifier = modifier.size(size)
    )
}
