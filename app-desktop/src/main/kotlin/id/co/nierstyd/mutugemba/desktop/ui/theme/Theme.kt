package id.co.nierstyd.mutugemba.desktop.ui.theme

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Shapes
import androidx.compose.material.Surface
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val MutuGembaShapes =
    Shapes(
        small =
            androidx.compose.foundation.shape
                .RoundedCornerShape(6.dp),
        medium =
            androidx.compose.foundation.shape
                .RoundedCornerShape(10.dp),
        large =
            androidx.compose.foundation.shape
                .RoundedCornerShape(16.dp),
    )

@Composable
fun MutuGembaTheme(content: @Composable () -> Unit) {
    val colors =
        lightColors(
            primary = BrandBlue,
            primaryVariant = BrandBlueDark,
            secondary = BrandRed,
            secondaryVariant = BrandRedDark,
            background = BackgroundTop,
            surface = NeutralSurface,
            onPrimary = NeutralSurface,
            onSecondary = NeutralSurface,
            onBackground = NeutralText,
            onSurface = NeutralText,
        )

    MaterialTheme(
        colors = colors,
        typography = mutuGembaTypography(),
        shapes = MutuGembaShapes,
    ) {
        Surface(color = colors.background) {
            content()
        }
    }
}
