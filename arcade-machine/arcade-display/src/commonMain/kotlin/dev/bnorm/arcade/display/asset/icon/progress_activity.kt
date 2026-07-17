package dev.bnorm.arcade.display.asset.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val progress_activity: ImageVector
    get() {
        if (_progress_activity != null) {
            return _progress_activity!!
        }
        _progress_activity =
            ImageVector.Builder(
                name = "progress_activity",
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
                        moveTo(8.13f, 21.21f)
                        quadTo(6.3f, 20.43f, 4.94f, 19.06f)
                        quadTo(3.58f, 17.7f, 2.79f, 15.88f)
                        reflectiveQuadTo(2f, 11.99f)
                        reflectiveQuadTo(2.79f, 8.11f)
                        reflectiveQuadTo(4.94f, 4.94f)
                        reflectiveQuadTo(8.13f, 2.79f)
                        reflectiveQuadTo(12f, 2f)
                        quadToRelative(0.43f, 0f, 0.71f, 0.29f)
                        reflectiveQuadTo(13f, 3f)
                        quadToRelative(0f, 0.42f, -0.29f, 0.71f)
                        reflectiveQuadTo(12f, 4f)
                        quadTo(8.68f, 4f, 6.34f, 6.34f)
                        reflectiveQuadTo(4f, 12f)
                        reflectiveQuadToRelative(2.34f, 5.66f)
                        reflectiveQuadTo(12f, 20f)
                        reflectiveQuadToRelative(5.66f, -2.34f)
                        reflectiveQuadTo(20f, 12f)
                        quadToRelative(0f, -0.43f, 0.29f, -0.71f)
                        reflectiveQuadTo(21f, 11f)
                        quadToRelative(0.43f, 0f, 0.71f, 0.29f)
                        reflectiveQuadTo(22f, 12f)
                        quadToRelative(0f, 2.05f, -0.79f, 3.88f)
                        reflectiveQuadToRelative(-2.15f, 3.19f)
                        reflectiveQuadToRelative(-3.17f, 2.15f)
                        reflectiveQuadTo(12.01f, 22f)
                        reflectiveQuadTo(8.13f, 21.21f)
                        close()
                    }
                }
                .build()
        return _progress_activity!!
    }

private var _progress_activity: ImageVector? = null
