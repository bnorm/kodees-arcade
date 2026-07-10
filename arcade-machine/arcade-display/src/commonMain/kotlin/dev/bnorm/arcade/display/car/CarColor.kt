package dev.bnorm.arcade.display.car

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

// From https://freesvg.org/blue-racing-car-vector-illustration
val CarColor: ImageVector
    get() {
        if (_CarColor != null) return _CarColor!!
        
        _CarColor = ImageVector.Builder(
            name = "CarColor",
            defaultWidth = 960.dp,
            defaultHeight = 476.dp,
            viewportWidth = 960f,
            viewportHeight = 476f
        ).apply {
            group(
                translationX = -52.937f,
                translationY = -486.69f,
            ) {
                path(
                    fill = SolidColor(Color(0xFF000000))
                ) {
                    moveToRelative(610.52f, 493.69f)
                    curveToRelative(-1.5086f, 0.009f, -4.7211f, 0.30611f, -6.4687f, 0.9375f)
                    lineToRelative(-3.5f, 1.5f)
                    lineToRelative(8.6562f, 35.844f)
                    lineToRelative(-124.81f, 0.28125f)
                    curveToRelative(-77.963f, 0.1654f, -166.52f, -11.504f, -232.93f, -9.5f)
                    curveToRelative(-66.412f, 2.0037f, -152.12f, 28f, -152.12f, 28f)
                    lineToRelative(-13.687f, 3.7812f)
                    curveToRelative(-19.251f, 5.2963f, -25.718f, 97.367f, -25.718f, 166.78f)
                    curveToRelative(0f, 1.113f, 0.02665f, 2.2531f, 0.03125f, 3.375f)
                    curveToRelative(-0.0046f, 1.1219f, -0.03125f, 2.262f, -0.03125f, 3.375f)
                    curveToRelative(0f, 69.414f, 6.4673f, 161.48f, 25.718f, 166.78f)
                    lineToRelative(13.687f, 3.7812f)
                    curveToRelative(0f, 0f, 85.711f, 25.996f, 152.12f, 28f)
                    curveToRelative(66.412f, 2.0037f, 154.97f, -9.6654f, 232.93f, -9.5f)
                    lineToRelative(124.81f, 0.28125f)
                    lineToRelative(-8.6562f, 35.844f)
                    lineToRelative(3.5f, 1.5f)
                    curveToRelative(1.7476f, 0.63139f, 4.96f, 0.92854f, 6.4687f, 0.9375f)
                    curveToRelative(0.84859f, 0.005f, 1.7551f, -0.0744f, 2.6875f, -0.25f)
                    curveToRelative(0.3107f, -0.0584f, 0.62352f, -0.14119f, 0.93749f, -0.21875f)
                    curveToRelative(0.30459f, -0.0754f, 0.63259f, -0.15686f, 0.93749f, -0.25f)
                    curveToRelative(0.62844f, -0.19269f, 1.2635f, -0.42206f, 1.875f, -0.6875f)
                    curveToRelative(1.8215f, -0.79201f, 3.5342f, -1.9028f, 4.7812f, -3.2812f)
                    curveToRelative(0.01f, -0.0107f, 0.0217f, -0.0208f, 0.0312f, -0.0312f)
                    curveToRelative(0.6248f, -0.69658f, 1.1123f, -1.4528f, 1.4687f, -2.2812f)
                    lineToRelative(12.156f, -31.25f)
                    lineToRelative(109.94f, 0.25f)
                    curveToRelative(23.9f, 11.942f, 45.511f, 10.719f, 73.593f, 10.719f)
                    curveToRelative(133.25f, 0f, 187.63002f, -86.586f, 187.00002f, -201.38f)
                    curveToRelative(0f, -0.7802f, -0.019f, -1.5656f, -0.031f, -2.3438f)
                    curveToRelative(0.012f, -0.77811f, 0.031f, -1.5636f, 0.031f, -2.3438f)
                    curveToRelative(0.6282f, -114.79f, -53.74902f, -201.38f, -187.00002f, -201.38f)
                    curveToRelative(-28.082f, 0f, -49.693f, -1.2236f, -73.593f, 10.719f)
                    lineToRelative(-109.94f, 0.25f)
                    lineToRelative(-12.156f, -31.25f)
                    curveToRelative(-0.35645f, -0.82844f, -0.84393f, -1.5847f, -1.4687f, -2.2812f)
                    curveToRelative(-0.01f, -0.0104f, -0.0213f, -0.0206f, -0.0312f, -0.0312f)
                    curveToRelative(-1.247f, -1.3785f, -2.9597f, -2.4892f, -4.7812f, -3.2812f)
                    curveToRelative(-0.61148f, -0.26544f, -1.2465f, -0.49481f, -1.875f, -0.6875f)
                    curveToRelative(-0.3049f, -0.0931f, -0.6329f, -0.1746f, -0.93749f, -0.25f)
                    curveToRelative(-0.31397f, -0.0776f, -0.62679f, -0.16035f, -0.93749f, -0.21875f)
                    curveToRelative(-0.93239f, -0.1756f, -1.8389f, -0.255f, -2.6875f, -0.25f)
                    close()
                }
            }
        }.build()
        
        return _CarColor!!
    }

private var _CarColor: ImageVector? = null

