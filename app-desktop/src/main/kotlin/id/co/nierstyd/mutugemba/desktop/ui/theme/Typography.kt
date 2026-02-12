package id.co.nierstyd.mutugemba.desktop.ui.theme

import androidx.compose.material.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.sp

@Suppress("LongMethod")
fun mutuGembaTypography(): Typography {
    val displayFont =
        FontFamily(
            Font("fonts/SpaceGrotesk[wght].ttf", FontWeight.Normal),
            Font("fonts/SpaceGrotesk[wght].ttf", FontWeight.SemiBold),
        )
    val bodyFont =
        FontFamily(
            Font("fonts/IBMPlexSans[wdth,wght].ttf", FontWeight.Normal),
            Font("fonts/IBMPlexSans[wdth,wght].ttf", FontWeight.Medium),
            Font("fonts/IBMPlexSans[wdth,wght].ttf", FontWeight.SemiBold),
            Font("fonts/IBMPlexSans[wdth,wght].ttf", FontWeight.Bold),
        )

    return Typography(
        h4 =
            TextStyle(
                fontFamily = displayFont,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                lineHeight = 34.sp,
                letterSpacing = 0.2.sp,
                color = NeutralText,
            ),
        h5 =
            TextStyle(
                fontFamily = displayFont,
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp,
                lineHeight = 28.sp,
                letterSpacing = 0.15.sp,
                color = NeutralText,
            ),
        h6 =
            TextStyle(
                fontFamily = displayFont,
                fontWeight = FontWeight.Medium,
                fontSize = 18.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.1.sp,
                color = NeutralText,
            ),
        subtitle1 =
            TextStyle(
                fontFamily = displayFont,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                color = NeutralText,
            ),
        subtitle2 =
            TextStyle(
                fontFamily = bodyFont,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = NeutralText,
            ),
        body1 =
            TextStyle(
                fontFamily = bodyFont,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = NeutralText,
            ),
        body2 =
            TextStyle(
                fontFamily = bodyFont,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = NeutralTextMuted,
            ),
        button =
            TextStyle(
                fontFamily = bodyFont,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                lineHeight = 18.sp,
            ),
        caption =
            TextStyle(
                fontFamily = bodyFont,
                fontWeight = FontWeight.Normal,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.15.sp,
                color = NeutralTextMuted,
            ),
    )
}
