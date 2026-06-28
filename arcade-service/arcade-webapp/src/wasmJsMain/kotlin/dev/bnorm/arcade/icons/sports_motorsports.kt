package dev.bnorm.arcade.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val sports_motorsports: ImageVector
    get() {
        if (_sports_motorsports != null) {
            return _sports_motorsports!!
        }
        _sports_motorsports =
            ImageVector.Builder(
                name = "sports_motorsports",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            )
                .apply {
                    path(
                        fill = SolidColor(Color.Black),
                        fillAlpha = 1f,
                        stroke = null,
                        strokeAlpha = 1f,
                        strokeLineWidth = 1f,
                        strokeLineCap = StrokeCap.Butt,
                        strokeLineJoin = StrokeJoin.Bevel,
                        strokeLineMiter = 1f,
                        pathFillType = PathFillType.NonZero,
                    ) {
                        moveTo(12f, 12f)
                        close()
                        moveToRelative(2f, 6f)
                        quadToRelative(2.5f, 0f, 4.25f, -1.75f)
                        reflectiveQuadTo(20f, 12f)
                        quadTo(20f, 9.48f, 18.16f, 7.74f)
                        reflectiveQuadTo(13.75f, 6f)
                        quadToRelative(-1.2f, 0f, -2.32f, 0.27f)
                        reflectiveQuadTo(9.25f, 7.1f)
                        lineToRelative(2.5f, 1f)
                        quadToRelative(1.03f, 0.42f, 1.64f, 1.31f)
                        reflectiveQuadTo(14f, 11.4f)
                        quadToRelative(0f, 1.5f, -1.04f, 2.55f)
                        reflectiveQuadTo(10.45f, 15f)
                        horizontalLineTo(4.05f)
                        quadTo(4f, 15.6f, 4f, 16.36f)
                        reflectiveQuadTo(4f, 18f)
                        horizontalLineTo(14f)
                        close()
                        moveTo(4.4f, 13f)
                        horizontalLineToRelative(6f)
                        quadToRelative(0.68f, 0f, 1.14f, -0.46f)
                        reflectiveQuadTo(12f, 11.4f)
                        quadToRelative(0f, -0.47f, -0.26f, -0.86f)
                        reflectiveQuadTo(11f, 9.95f)
                        lineTo(7.3f, 8.45f)
                        quadTo(6.25f, 9.38f, 5.51f, 10.55f)
                        quadTo(4.78f, 11.73f, 4.4f, 13f)
                        close()
                        moveTo(14f, 20f)
                        horizontalLineTo(4f)
                        quadTo(3.18f, 20f, 2.59f, 19.41f)
                        reflectiveQuadTo(2f, 18f)
                        verticalLineTo(15.75f)
                        quadTo(2f, 13.3f, 2.93f, 11.16f)
                        quadTo(3.85f, 9.02f, 5.44f, 7.44f)
                        reflectiveQuadTo(9.18f, 4.93f)
                        reflectiveQuadTo(13.75f, 4f)
                        quadToRelative(1.7f, 0f, 3.2f, 0.63f)
                        reflectiveQuadToRelative(2.63f, 1.71f)
                        quadToRelative(1.13f, 1.09f, 1.77f, 2.54f)
                        reflectiveQuadTo(22f, 12f)
                        quadToRelative(0f, 1.65f, -0.63f, 3.11f)
                        reflectiveQuadToRelative(-1.71f, 2.55f)
                        reflectiveQuadToRelative(-2.55f, 1.71f)
                        reflectiveQuadTo(14f, 20f)
                        close()
                    }
                }
                .build()
        return _sports_motorsports!!
    }

private var _sports_motorsports: ImageVector? = null
