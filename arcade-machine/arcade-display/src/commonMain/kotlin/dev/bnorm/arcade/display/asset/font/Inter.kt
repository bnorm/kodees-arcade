package dev.bnorm.arcade.display.asset.font

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import dev.bnorm.arcade.arcade_display.generated.resources.Inter_Black
import dev.bnorm.arcade.arcade_display.generated.resources.Inter_Bold
import dev.bnorm.arcade.arcade_display.generated.resources.Inter_ExtraBold
import dev.bnorm.arcade.arcade_display.generated.resources.Inter_ExtraLight
import dev.bnorm.arcade.arcade_display.generated.resources.Inter_Light
import dev.bnorm.arcade.arcade_display.generated.resources.Inter_Medium
import dev.bnorm.arcade.arcade_display.generated.resources.Inter_Regular
import dev.bnorm.arcade.arcade_display.generated.resources.Inter_SemiBold
import dev.bnorm.arcade.arcade_display.generated.resources.Inter_Thin
import dev.bnorm.arcade.arcade_display.generated.resources.Res
import org.jetbrains.compose.resources.Font

val Inter
    @Composable
    get() = FontFamily(
        Font(
            resource = Res.font.Inter_Thin,
            weight = FontWeight.Thin,
            style = FontStyle.Normal
        ),
        Font(
            resource = Res.font.Inter_ExtraLight,
            weight = FontWeight.ExtraLight,
            style = FontStyle.Normal
        ),
        Font(
            resource = Res.font.Inter_Light,
            weight = FontWeight.Light,
            style = FontStyle.Normal
        ),
        Font(
            resource = Res.font.Inter_Regular,
            weight = FontWeight.Normal,
            style = FontStyle.Normal
        ),
        Font(
            resource = Res.font.Inter_Medium,
            weight = FontWeight.Medium,
            style = FontStyle.Normal
        ),
        Font(
            resource = Res.font.Inter_SemiBold,
            weight = FontWeight.SemiBold,
            style = FontStyle.Normal
        ),
        Font(
            resource = Res.font.Inter_Bold,
            weight = FontWeight.Bold,
            style = FontStyle.Normal
        ),
        Font(
            resource = Res.font.Inter_ExtraBold,
            weight = FontWeight.ExtraBold,
            style = FontStyle.Normal
        ),
        Font(
            resource = Res.font.Inter_Black,
            weight = FontWeight.Black,
            style = FontStyle.Normal
        ),
    )
