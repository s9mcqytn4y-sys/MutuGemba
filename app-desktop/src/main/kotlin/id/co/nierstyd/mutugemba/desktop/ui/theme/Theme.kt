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
                .RoundedCornerShape(8.dp),
        medium =
            androidx.compose.foundation.shape
                .RoundedCornerShape(12.dp),
        large =
            androidx.compose.foundation.shape
                .RoundedCornerShape(0.dp),
    )

@Composable
fun MutuGembaTheme(content: @Composable () -> Unit) {
    val colors =
        lightColors(
            primary = BrandRed,
            primaryVariant = BrandRedDark,
            secondary = BrandBlue,
            secondaryVariant = BrandBlueDark,
            background = NeutralLight,
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
