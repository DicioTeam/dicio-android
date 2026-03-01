package org.stypox.dicio.ui.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.stypox.dicio.ui.theme.AppTheme

/**
 * Generated with https://github.com/rafaeltonholo/svg-to-compose/
 * based on meta/logo_with_background.svg.
 */
val DicioSquircleIcon: ImageVector
    get() {
        val current = _logo
        if (current != null) return current

        return ImageVector.Builder(
            name = "DicioSquircleIcon",
            defaultWidth = 100.0.dp,
            defaultHeight = 100.0.dp,
            viewportWidth = 100.0f,
            viewportHeight = 100.0f,
        ).apply {

            // M0 50 C0 15 15 0 50 0 s50 15 50 50 -15 50 -50 50 S0 85 0 50
            path(
                fill = SolidColor(Color(0xFFDAEC21)),
            ) {
                // M 0 50
                moveTo(x = 0.0f, y = 50.0f)
                // C 0 15 15 0 50 0
                curveTo(
                    x1 = 0.0f,
                    y1 = 15.0f,
                    x2 = 15.0f,
                    y2 = 0.0f,
                    x3 = 50.0f,
                    y3 = 0.0f,
                )
                // s 50 15 50 50
                reflectiveCurveToRelative(
                    dx1 = 50.0f,
                    dy1 = 15.0f,
                    dx2 = 50.0f,
                    dy2 = 50.0f,
                )
                // s -15 50 -50 50
                reflectiveCurveToRelative(
                    dx1 = -15.0f,
                    dy1 = 50.0f,
                    dx2 = -50.0f,
                    dy2 = 50.0f,
                )
                // S 0 85 0 50
                reflectiveCurveTo(
                    x1 = 0.0f,
                    y1 = 85.0f,
                    x2 = 0.0f,
                    y2 = 50.0f,
                )
            }
            // M32.73 17.4 c-5.9 42.09 -1.83 50.85 2.9 53.77 -14.64 -1.4 -11.6 10.07 -2.3 11.13 55.78 6.34 63.52 -66.13 -.6 -64.9 m20.63 13.82 c3.47 0 6.27 2.8 6.27 6.26 L59.6 50.01 a6.26 6.26 0 1 1 -12.5 0 V37.48 c-.01 -3.46 2.79 -6.26 6.25 -6.26 m-14.62 18.8 h3.55 c0 6.26 5.3 10.64 11.07 10.64 5.76 0 11.07 -4.38 11.07 -10.65 h3.55 c0 7.15 -5.68 13.03 -12.53 14.04 v6.85 h-4.18 v-6.85 C44.42 63.02 38.74 57.13 38.74 50
            path(
                fill = SolidColor(Color(0xFF006800)),
            ) {
                // M 32.73 17.4
                moveTo(x = 32.73f, y = 17.4f)
                // c -5.9 42.09 -1.83 50.85 2.9 53.77
                curveToRelative(
                    dx1 = -5.9f,
                    dy1 = 42.09f,
                    dx2 = -1.83f,
                    dy2 = 50.85f,
                    dx3 = 2.9f,
                    dy3 = 53.77f,
                )
                // c -14.64 -1.4 -11.6 10.07 -2.3 11.13
                curveToRelative(
                    dx1 = -14.64f,
                    dy1 = -1.4f,
                    dx2 = -11.6f,
                    dy2 = 10.07f,
                    dx3 = -2.3f,
                    dy3 = 11.13f,
                )
                // c 55.78 6.34 63.52 -66.13 -0.6 -64.9
                curveToRelative(
                    dx1 = 55.78f,
                    dy1 = 6.34f,
                    dx2 = 63.52f,
                    dy2 = -66.13f,
                    dx3 = -0.6f,
                    dy3 = -64.9f,
                )
                // m 20.63 13.82
                moveToRelative(dx = 20.63f, dy = 13.82f)
                // c 3.47 0 6.27 2.8 6.27 6.26
                curveToRelative(
                    dx1 = 3.47f,
                    dy1 = 0.0f,
                    dx2 = 6.27f,
                    dy2 = 2.8f,
                    dx3 = 6.27f,
                    dy3 = 6.26f,
                )
                // L 59.6 50.01
                lineTo(x = 59.6f, y = 50.01f)
                // a 6.26 6.26 0 1 1 -12.5 0
                arcToRelative(
                    a = 6.26f,
                    b = 6.26f,
                    theta = 0.0f,
                    isMoreThanHalf = true,
                    isPositiveArc = true,
                    dx1 = -12.5f,
                    dy1 = 0.0f,
                )
                // V 37.48
                verticalLineTo(y = 37.48f)
                // c -0.01 -3.46 2.79 -6.26 6.25 -6.26
                curveToRelative(
                    dx1 = -0.01f,
                    dy1 = -3.46f,
                    dx2 = 2.79f,
                    dy2 = -6.26f,
                    dx3 = 6.25f,
                    dy3 = -6.26f,
                )
                // m -14.62 18.8
                moveToRelative(dx = -14.62f, dy = 18.8f)
                // h 3.55
                horizontalLineToRelative(dx = 3.55f)
                // c 0 6.26 5.3 10.64 11.07 10.64
                curveToRelative(
                    dx1 = 0.0f,
                    dy1 = 6.26f,
                    dx2 = 5.3f,
                    dy2 = 10.64f,
                    dx3 = 11.07f,
                    dy3 = 10.64f,
                )
                // c 5.76 0 11.07 -4.38 11.07 -10.65
                curveToRelative(
                    dx1 = 5.76f,
                    dy1 = 0.0f,
                    dx2 = 11.07f,
                    dy2 = -4.38f,
                    dx3 = 11.07f,
                    dy3 = -10.65f,
                )
                // h 3.55
                horizontalLineToRelative(dx = 3.55f)
                // c 0 7.15 -5.68 13.03 -12.53 14.04
                curveToRelative(
                    dx1 = 0.0f,
                    dy1 = 7.15f,
                    dx2 = -5.68f,
                    dy2 = 13.03f,
                    dx3 = -12.53f,
                    dy3 = 14.04f,
                )
                // v 6.85
                verticalLineToRelative(dy = 6.85f)
                // h -4.18
                horizontalLineToRelative(dx = -4.18f)
                // v -6.85
                verticalLineToRelative(dy = -6.85f)
                // C 44.42 63.02 38.74 57.13 38.74 50
                curveTo(
                    x1 = 44.42f,
                    y1 = 63.02f,
                    x2 = 38.74f,
                    y2 = 57.13f,
                    x3 = 38.74f,
                    y3 = 50.0f,
                )
            }
        }.build().also { _logo = it }
    }

@Preview
@Composable
private fun IconPreview() {
    AppTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                imageVector = DicioSquircleIcon,
                contentDescription = null,
                modifier = Modifier
                    .width((100.0).dp)
                    .height((100.0).dp),
            )
        }
    }
}

@Suppress("ObjectPropertyName")
private var _logo: ImageVector? = null
