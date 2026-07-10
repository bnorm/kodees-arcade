package dev.bnorm.arcade.display.car

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

// From https://freesvg.org/blue-racing-car-vector-illustration
val CarTop: ImageVector
    get() {
        if (_CarTop != null) return _CarTop!!

        _CarTop = ImageVector.Builder(
            name = "CarTop",
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
                    moveToRelative(400.34f, 855.24f)
                    curveToRelative(-33.364f, 0f, -65.307f, 1.8f, -94.811f, 5.0625f)
                    curveToRelative(25.66f, 48.714f, 97.985f, 30.265f, 205.56f, 31.531f)
                    curveToRelative(49.686f, 0.58471f, 89.543f, 1.8793f, 121.53f, 2.375f)
                    curveToRelative(-47.16f, -23.334f, -133.53f, -38.969f, -232.28f, -38.969f)
                    close()
                }
                path(
                    fill = SolidColor(Color(0xFF000000))
                ) {
                    moveToRelative(400.34f, 855.24f)
                    curveToRelative(-3.2064f, 0f, -6.3831f, 0.0295f, -9.5624f, 0.0625f)
                    curveToRelative(0.81825f, 16.171f, 6.4281f, 30.257f, 14.594f, 38.844f)
                    curveToRelative(4.6714f, -0.0756f, 9.4951f, -0.19655f, 14.437f, -0.34375f)
                    curveToRelative(-8.5657f, -8.1923f, -14.593f, -22.228f, -15.719f, -38.562f)
                    curveToRelative(-1.2512f, -0.005f, -2.4947f, 0f, -3.75f, 0f)
                    close()
                }
                path(
                    fill = SolidColor(Color(0xFF000000))
                ) {
                    moveToRelative(989.02f, 827.5f)
                    lineToRelative(-5.0937f, 0.59375f)
                    curveToRelative(-21.545f, 2.5127f, -37.688f, 25.979f, -39.281f, 54.531f)
                    lineToRelative(-0.37499f, 7.125f)
                    lineToRelative(5.2499f, -4.8438f)
                    curveToRelative(15.889f, -14.68f, 28.303f, -32.507f, 37.406f, -52.75f)
                    lineToRelative(2.09f, -4.65f)
                    close()
                }
                path(
                    fill = SolidColor(Color(0xFF000000))
                ) {
                    moveToRelative(783.47f, 838.5f)
                    curveToRelative(0f, 0f, 79.677f, -22.596f, 105.38f, -31.982f)
                    curveToRelative(26.839f, -9.8018f, 98.859f, -39.146f, 98.859f, -39.146f)
                    curveToRelative(0f, 0f, -8.7409f, 42.47f, -30.483f, 57.918f)
                    curveToRelative(-77.23f, 54.87f, -232.69f, 53.85f, -232.69f, 53.85f)
                }
                path(
                    fill = SolidColor(Color(0xFF000000))
                ) {
                    moveToRelative(869.97f, 817.84f)
                    lineToRelative(-4.4374f, 2.3438f)
                    curveToRelative(0.98912f, 1.1568f, 1.7955f, 2.4286f, 2.375f, 3.8438f)
                    curveToRelative(4.7979f, 11.717f, -10.736f, 29.236f, -26.875f, 35.781f)
                    curveToRelative(-0.51675f, 0.20958f, -1.8129f, 0.84066f, -3.4062f, 1.6562f)
                    lineToRelative(13.625f, -3.875f)
                    curveToRelative(17.306f, -8.4576f, 27.47f, -23.082f, 23f, -34f)
                    curveToRelative(-0.91615f, -2.2373f, -2.3752f, -4.1661f, -4.2812f, -5.75f)
                    close()
                }
                path(
                    fill = SolidColor(Color(0xFF000000))
                ) {
                    moveToRelative(878.55f, 813.38f)
                    lineToRelative(-4.4375f, 2.3438f)
                    curveToRelative(0.98913f, 1.1568f, 1.7955f, 2.4286f, 2.375f, 3.8438f)
                    curveToRelative(4.7979f, 11.717f, -10.736f, 29.236f, -26.875f, 35.781f)
                    curveToRelative(-0.51676f, 0.20958f, -1.8129f, 0.84066f, -3.4062f, 1.6562f)
                    lineToRelative(13.625f, -3.875f)
                    curveToRelative(17.306f, -8.4576f, 27.47f, -23.082f, 23f, -34f)
                    curveToRelative(-0.91615f, -2.2373f, -2.3752f, -4.1661f, -4.2812f, -5.75f)
                    close()
                }
                path(
                    fill = SolidColor(Color(0xFF000000))
                ) {
                    moveToRelative(884.74f, 811.96f)
                    lineToRelative(-4.4374f, 2.3438f)
                    curveToRelative(0.98913f, 1.1568f, 1.7955f, 2.4286f, 2.375f, 3.8438f)
                    curveToRelative(4.7979f, 11.717f, -10.736f, 29.236f, -26.875f, 35.781f)
                    curveToRelative(-0.51675f, 0.20958f, -1.8129f, 0.84066f, -3.4062f, 1.6562f)
                    lineToRelative(13.625f, -3.875f)
                    curveToRelative(17.306f, -8.4576f, 27.47f, -23.082f, 23f, -34f)
                    curveToRelative(-0.91615f, -2.2373f, -2.3752f, -4.1661f, -4.2812f, -5.75f)
                    close()
                }
                path(
                    fill = SolidColor(Color(0xFF000000))
                ) {
                    moveToRelative(901.65f, 807.69f)
                    lineToRelative(-6.1874f, 1.8438f)
                    curveToRelative(0.96015f, 1.7128f, 1.6545f, 3.5323f, 2.0312f, 5.4688f)
                    curveToRelative(3.1194f, 16.034f, -20.962f, 34.284f, -43.031f, 38.5f)
                    curveToRelative(-3.395f, 0.64864f, -28.884f, 8.576f, -32.158f, 8.8044f)
                    verticalLineToRelative(4.125f)
                    lineToRelative(41.439f, -12.148f)
                    curveToRelative(26.285f, -5.4963f, 44.949f, -22.448f, 41.875f, -38.25f)
                    curveToRelative(-0.59564f, -3.0616f, -1.956f, -5.8595f, -3.9687f, -8.3438f)
                    close()
                }
                path(
                    fill = SolidColor(Color(0xFF000000))
                ) {
                    moveToRelative(901.65f, 807.69f)
                    lineToRelative(-6.1874f, 1.8438f)
                    curveToRelative(0.96015f, 1.7128f, 1.6545f, 3.5323f, 2.0312f, 5.4688f)
                    curveToRelative(3.1194f, 16.034f, -20.962f, 34.284f, -43.031f, 38.5f)
                    curveToRelative(-3.395f, 0.64864f, -28.884f, 8.576f, -32.158f, 8.8044f)
                    verticalLineToRelative(4.125f)
                    lineToRelative(41.439f, -12.148f)
                    curveToRelative(26.285f, -5.4963f, 44.949f, -22.448f, 41.875f, -38.25f)
                    curveToRelative(-0.59564f, -3.0616f, -1.956f, -5.8595f, -3.9687f, -8.3438f)
                    close()
                }
                path(
                    fill = SolidColor(Color(0xFF000000))
                ) {
                    moveToRelative(857.12f, 822.46f)
                    lineToRelative(-3.9641f, 2.0937f)
                    curveToRelative(0.88361f, 1.0334f, 1.604f, 2.1696f, 2.1216f, 3.4337f)
                    curveToRelative(4.2861f, 10.467f, -9.5906f, 26.117f, -24.008f, 31.964f)
                    curveToRelative(-0.46163f, 0.18723f, -1.6195f, 0.75098f, -3.0428f, 1.4796f)
                    lineToRelative(12.171f, -3.4616f)
                    curveToRelative(15.46f, -7.5554f, 24.54f, -20.62f, 20.546f, -30.373f)
                    curveToRelative(-0.81842f, -1.9987f, -2.1218f, -3.7216f, -3.8245f, -5.1366f)
                    close()
                }
                path(
                    fill = SolidColor(Color(0xFF000000))
                ) {
                    moveToRelative(843.32f, 826.03f)
                    lineToRelative(-3.9641f, 2.0937f)
                    curveToRelative(0.88361f, 1.0334f, 1.604f, 2.1696f, 2.1216f, 3.4337f)
                    curveToRelative(4.2861f, 10.467f, -9.5906f, 26.117f, -24.008f, 31.964f)
                    curveToRelative(-0.46162f, 0.18723f, -1.6195f, 0.75098f, -3.0428f, 1.4796f)
                    lineToRelative(12.171f, -3.4616f)
                    curveToRelative(15.46f, -7.5554f, 24.54f, -20.62f, 20.546f, -30.373f)
                    curveToRelative(-0.81842f, -1.9987f, -2.1218f, -3.7216f, -3.8245f, -5.1366f)
                    close()
                }
                path(
                    fill = SolidColor(Color(0xFF000000))
                ) {
                    moveToRelative(233.27f, 845.72f)
                    curveToRelative(8.293f, -2.0234f, 15.486f, -1.4788f, 19.797f, 5.7872f)
                    lineToRelative(-2.4934f, 17.897f)
                    curveToRelative(-6.8751f, 6.1732f, -13.75f, 4.9509f, -20.625f, 0.15543f)
                    lineToRelative(3.3212f, -23.839f)
                    close()
                }
                path(
                    fill = SolidColor(Color(0xFF000000))
                ) {
                    moveToRelative(253.54f, 848.99f)
                    curveToRelative(8.1502f, -1.2102f, 15.167f, -0.5728f, 18.843f, 5.5081f)
                    lineToRelative(-2.3731f, 17.034f)
                    curveToRelative(-6.4839f, 2.9748f, -12.983f, 5.2096f, -19.631f, 0.14793f)
                    lineToRelative(3.1611f, -22.69f)
                    close()
                }
                path(
                    fill = SolidColor(Color(0xFF000000))
                ) {
                    moveToRelative(400.34f, 852.75f)
                    curveToRelative(-33.454f, 0f, -65.492f, 1.7894f, -95.093f, 5.0625f)
                    lineToRelative(-3.6562f, 0.40625f)
                    lineToRelative(1.7187f, 3.25f)
                    curveToRelative(6.6711f, 12.664f, 16.562f, 21.113f, 29.062f, 26.438f)
                    curveToRelative(12.501f, 5.3241f, 27.572f, 7.6126f, 45.093f, 8.4375f)
                    curveToRelative(35.042f, 1.6498f, 79.954f, -2.6312f, 133.59f, -2f)
                    curveToRelative(49.659f, 0.58438f, 89.508f, 1.8787f, 121.53f, 2.375f)
                    lineToRelative(1.125f, -4.75f)
                    curveToRelative(-47.84f, -23.68f, -134.34f, -39.22f, -233.36f, -39.22f)
                    close()
                    moveToRelative(0f, 5f)
                    curveToRelative(91.169f, 0f, 171.75f, 13.479f, 220.09f, 33.719f)
                    curveToRelative(-29.952f, -0.58241f, -65.212f, -1.606f, -109.31f, -2.125f)
                    curveToRelative(-53.937f, -0.63473f, -98.976f, 3.6522f, -133.4f, 2.0312f)
                    curveToRelative(-17.214f, -0.81046f, -31.767f, -3.1054f, -43.406f, -8.0625f)
                    curveToRelative(-10.453f, -4.4521f, -18.485f, -11.154f, -24.5f, -20.906f)
                    curveToRelative(28.307f, -2.9831f, 58.735f, -4.6562f, 90.53f, -4.6562f)
                    close()
                }
                path(
                    fill = SolidColor(Color(0xFF000000))
                ) {
                    moveToRelative(260.5f, 607.38f)
                    lineToRelative(-77.749f, 12.469f)
                    curveToRelative(-27.15f, 4.3542f, -48.947f, 48.773f, -50.999f, 104.84f)
                    curveToRelative(2.0523f, 56.071f, 23.849f, 100.49f, 50.999f, 104.84f)
                    lineToRelative(77.749f, 12.469f)
                    curveToRelative(13.296f, 0f, 24f, -10.704f, 24f, -24f)
                    verticalLineToRelative(-186.62f)
                    curveToRelative(0f, -13.296f, -10.704f, -24f, -24f, -24f)
                    close()
                }
                path(
                    fill = SolidColor(Color(0xFF000000))
                ) {
                    moveToRelative(691.96f, 573.16f)
                    curveToRelative(-2.9692f, 0f, -5.8933f, 0.33215f, -8.7812f, 0.96875f)
                    curveToRelative(-0.0104f, -0.01f, -0.0208f, -0.021f, -0.0312f, -0.0312f)
                    lineToRelative(-63.843f, 12.312f)
                    curveToRelative(-17.728f, 6.6047f, -32f, 14.272f, -32f, 32f)
                    verticalLineToRelative(212.56f)
                    curveToRelative(0f, 17.728f, 14.272f, 25.395f, 32f, 32f)
                    lineToRelative(63.843f, 12.312f)
                    curveToRelative(0.0105f, -0.0102f, 0.0208f, -0.0211f, 0.0312f, -0.0312f)
                    curveToRelative(2.8879f, 0.6366f, 5.812f, 0.96875f, 8.7812f, 0.96875f)
                    curveToRelative(45.395f, 0f, 82.198f, -57.363f, 82.312f, -151.53f)
                    curveToRelative(-0.11408f, -94.169f, -36.916f, -151.53f, -82.312f, -151.53f)
                    close()
                }
                path(
                    fill = SolidColor(Color(0xFF000000))
                ) {
                    moveToRelative(400.34f, 594.15f)
                    curveToRelative(-33.364f, 0f, -65.307f, -1.8f, -94.811f, -5.0625f)
                    curveToRelative(25.66f, -48.714f, 97.985f, -30.265f, 205.56f, -31.531f)
                    curveToRelative(49.686f, -0.58471f, 89.543f, -1.8793f, 121.53f, -2.375f)
                    curveToRelative(-47.16f, 23.334f, -133.53f, 38.969f, -232.28f, 38.969f)
                    close()
                }
                path(
                    fill = SolidColor(Color(0xFF000000))
                ) {
                    moveToRelative(400.34f, 594.15f)
                    curveToRelative(-3.2064f, 0f, -6.3831f, -0.0295f, -9.5624f, -0.0625f)
                    curveToRelative(0.81825f, -16.171f, 6.4281f, -30.257f, 14.594f, -38.844f)
                    curveToRelative(4.6714f, 0.0756f, 9.4951f, 0.19655f, 14.437f, 0.34375f)
                    curveToRelative(-8.5657f, 8.1923f, -14.593f, 22.228f, -15.719f, 38.562f)
                    curveToRelative(-1.2512f, 0.005f, -2.4947f, 0f, -3.75f, 0f)
                    close()
                }
                path(
                    fill = SolidColor(Color(0xFF000000))
                ) {
                    moveToRelative(989.02f, 621.89f)
                    lineToRelative(-5.0937f, -0.59375f)
                    curveToRelative(-21.545f, -2.5127f, -37.688f, -25.979f, -39.281f, -54.531f)
                    lineToRelative(-0.37499f, -7.125f)
                    lineToRelative(5.2499f, 4.8438f)
                    curveToRelative(15.889f, 14.68f, 28.303f, 32.507f, 37.406f, 52.75f)
                    lineToRelative(2.0937f, 4.6562f)
                    close()
                }
                path(
                    fill = SolidColor(Color(0xFF000000))
                ) {
                    moveToRelative(783.47f, 610.89f)
                    curveToRelative(0f, 0f, 79.677f, 22.596f, 105.38f, 31.982f)
                    curveToRelative(26.839f, 9.8018f, 98.859f, 39.146f, 98.859f, 39.146f)
                    curveToRelative(0f, 0f, -8.7409f, -42.47f, -30.483f, -57.918f)
                    curveToRelative(-77.23f, -54.87f, -232.69f, -53.86f, -232.69f, -53.86f)
                }
                path(
                    fill = SolidColor(Color(0xFF000000))
                ) {
                    moveToRelative(869.97f, 631.55f)
                    lineToRelative(-4.4374f, -2.3438f)
                    curveToRelative(0.98912f, -1.1568f, 1.7955f, -2.4286f, 2.375f, -3.8438f)
                    curveToRelative(4.7979f, -11.717f, -10.736f, -29.236f, -26.875f, -35.781f)
                    curveToRelative(-0.51675f, -0.20958f, -1.8129f, -0.84066f, -3.4062f, -1.6562f)
                    lineToRelative(13.625f, 3.875f)
                    curveToRelative(17.306f, 8.4576f, 27.47f, 23.082f, 23f, 34f)
                    curveToRelative(-0.91615f, 2.2373f, -2.3752f, 4.1661f, -4.2812f, 5.75f)
                    close()
                }
                path(
                    fill = SolidColor(Color(0xFF000000))
                ) {
                    moveToRelative(878.55f, 636.01f)
                    lineToRelative(-4.4375f, -2.3438f)
                    curveToRelative(0.98913f, -1.1568f, 1.7955f, -2.4286f, 2.375f, -3.8438f)
                    curveToRelative(4.7979f, -11.717f, -10.736f, -29.236f, -26.875f, -35.781f)
                    curveToRelative(-0.51676f, -0.20958f, -1.8129f, -0.84066f, -3.4062f, -1.6562f)
                    lineToRelative(13.625f, 3.875f)
                    curveToRelative(17.306f, 8.4576f, 27.47f, 23.082f, 23f, 34f)
                    curveToRelative(-0.91615f, 2.2373f, -2.3752f, 4.1661f, -4.2812f, 5.75f)
                    close()
                }
                path(
                    fill = SolidColor(Color(0xFF000000))
                ) {
                    moveToRelative(884.74f, 637.42f)
                    lineToRelative(-4.4374f, -2.3438f)
                    curveToRelative(0.98913f, -1.1568f, 1.7955f, -2.4286f, 2.375f, -3.8438f)
                    curveToRelative(4.7979f, -11.717f, -10.736f, -29.236f, -26.875f, -35.781f)
                    curveToRelative(-0.51675f, -0.20958f, -1.8129f, -0.84066f, -3.4062f, -1.6562f)
                    lineToRelative(13.625f, 3.875f)
                    curveToRelative(17.306f, 8.4576f, 27.47f, 23.082f, 23f, 34f)
                    curveToRelative(-0.91615f, 2.2373f, -2.3752f, 4.1661f, -4.2812f, 5.75f)
                    close()
                }
                path(
                    fill = SolidColor(Color(0xFF000000))
                ) {
                    moveToRelative(901.65f, 641.7f)
                    lineToRelative(-6.1874f, -1.8438f)
                    curveToRelative(0.96015f, -1.7128f, 1.6545f, -3.5323f, 2.0312f, -5.4688f)
                    curveToRelative(3.1194f, -16.034f, -20.962f, -34.284f, -43.031f, -38.5f)
                    curveToRelative(-3.395f, -0.64864f, -28.884f, -8.576f, -32.158f, -8.8044f)
                    verticalLineToRelative(-4.125f)
                    lineToRelative(41.439f, 12.148f)
                    curveToRelative(26.285f, 5.4963f, 44.949f, 22.448f, 41.875f, 38.25f)
                    curveToRelative(-0.59564f, 3.0616f, -1.956f, 5.8595f, -3.9687f, 8.3438f)
                    close()
                }
                path(
                    fill = SolidColor(Color(0xFF000000))
                ) {
                    moveToRelative(901.65f, 641.7f)
                    lineToRelative(-6.1874f, -1.8438f)
                    curveToRelative(0.96015f, -1.7128f, 1.6545f, -3.5323f, 2.0312f, -5.4688f)
                    curveToRelative(3.1194f, -16.034f, -20.962f, -34.284f, -43.031f, -38.5f)
                    curveToRelative(-3.395f, -0.64864f, -28.884f, -8.576f, -32.158f, -8.8044f)
                    verticalLineToRelative(-4.125f)
                    lineToRelative(41.439f, 12.148f)
                    curveToRelative(26.285f, 5.4963f, 44.949f, 22.448f, 41.875f, 38.25f)
                    curveToRelative(-0.59564f, 3.0616f, -1.956f, 5.8595f, -3.9687f, 8.3438f)
                    close()
                }
                path(
                    fill = SolidColor(Color(0xFF000000))
                ) {
                    moveToRelative(857.12f, 626.93f)
                    lineToRelative(-3.9641f, -2.0937f)
                    curveToRelative(0.88361f, -1.0334f, 1.604f, -2.1696f, 2.1216f, -3.4337f)
                    curveToRelative(4.2861f, -10.467f, -9.5906f, -26.117f, -24.008f, -31.964f)
                    curveToRelative(-0.46163f, -0.18723f, -1.6195f, -0.75098f, -3.0428f, -1.4796f)
                    lineToRelative(12.171f, 3.4616f)
                    curveToRelative(15.46f, 7.5554f, 24.54f, 20.62f, 20.546f, 30.373f)
                    curveToRelative(-0.81842f, 1.9987f, -2.1218f, 3.7216f, -3.8245f, 5.1366f)
                    close()
                }
                path(
                    fill = SolidColor(Color(0xFF000000))
                ) {
                    moveToRelative(843.32f, 623.36f)
                    lineToRelative(-3.9641f, -2.0937f)
                    curveToRelative(0.88361f, -1.0334f, 1.604f, -2.1696f, 2.1216f, -3.4337f)
                    curveToRelative(4.2861f, -10.467f, -9.5906f, -26.117f, -24.008f, -31.964f)
                    curveToRelative(-0.46162f, -0.18723f, -1.6195f, -0.75098f, -3.0428f, -1.4796f)
                    lineToRelative(12.171f, 3.4616f)
                    curveToRelative(15.46f, 7.5554f, 24.54f, 20.62f, 20.546f, 30.373f)
                    curveToRelative(-0.81842f, 1.9987f, -2.1218f, 3.7216f, -3.8245f, 5.1366f)
                    close()
                }
                path(
                    fill = SolidColor(Color(0xFF000000))
                ) {
                    moveToRelative(233.27f, 603.66f)
                    curveToRelative(8.293f, 2.0234f, 15.486f, 1.4788f, 19.797f, -5.7872f)
                    lineToRelative(-2.4934f, -17.897f)
                    curveToRelative(-6.8751f, -6.1732f, -13.75f, -4.9509f, -20.625f, -0.15543f)
                    lineToRelative(3.3212f, 23.839f)
                    close()
                }
                path(
                    fill = SolidColor(Color(0xFF000000))
                ) {
                    moveToRelative(253.54f, 600.4f)
                    curveToRelative(8.1502f, 1.2102f, 15.167f, 0.5728f, 18.843f, -5.5081f)
                    lineToRelative(-2.3731f, -17.034f)
                    curveToRelative(-6.4839f, -2.9748f, -12.983f, -5.2096f, -19.631f, -0.14793f)
                    lineToRelative(3.1611f, 22.69f)
                    close()
                }
                path(
                    fill = SolidColor(Color(0xFF000000))
                ) {
                    moveToRelative(400.34f, 596.64f)
                    curveToRelative(-33.454f, 0f, -65.492f, -1.7894f, -95.093f, -5.0625f)
                    lineToRelative(-3.6562f, -0.40625f)
                    lineToRelative(1.7187f, -3.25f)
                    curveToRelative(6.6711f, -12.664f, 16.562f, -21.113f, 29.062f, -26.438f)
                    curveToRelative(12.501f, -5.3241f, 27.572f, -7.6126f, 45.093f, -8.4375f)
                    curveToRelative(35.042f, -1.6498f, 79.954f, 2.6312f, 133.59f, 2f)
                    curveToRelative(49.659f, -0.58438f, 89.508f, -1.8787f, 121.53f, -2.375f)
                    lineToRelative(1.125f, 4.75f)
                    curveToRelative(-47.849f, 23.675f, -134.36f, 39.219f, -233.37f, 39.219f)
                    close()
                    moveToRelative(0f, -5f)
                    curveToRelative(91.169f, 0f, 171.75f, -13.479f, 220.09f, -33.719f)
                    curveToRelative(-29.952f, 0.58241f, -65.212f, 1.606f, -109.31f, 2.125f)
                    curveToRelative(-53.937f, 0.63473f, -98.976f, -3.6522f, -133.4f, -2.0312f)
                    curveToRelative(-17.214f, 0.81046f, -31.767f, 3.1054f, -43.406f, 8.0625f)
                    curveToRelative(-10.453f, 4.4521f, -18.485f, 11.154f, -24.5f, 20.906f)
                    curveToRelative(28.307f, 2.9831f, 58.735f, 4.6562f, 90.53f, 4.6562f)
                    close()
                }
            }
        }.build()

        return _CarTop!!
    }

private var _CarTop: ImageVector? = null

