package id.co.nierstyd.mutugemba.desktop.ui.theme

import androidx.compose.material.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

fun mutuGembaTypography(): Typography {
    val displayFont = FontFamily.Serif
    val bodyFont = FontFamily.SansSerif

    return Typography(
        h4 =
            TextStyle(
                fontFamily = displayFont,
                fontWeight = FontWeight.Bold,
                fontSize = 30.sp,
                color = NeutralText,
            ),
        h5 =
            TextStyle(
                fontFamily = displayFont,
                fontWeight = FontWeight.SemiBold,
                fontSize = 24.sp,
                color = NeutralText,
            ),
        h6 =
            TextStyle(
                fontFamily = displayFont,
                fontWeight = FontWeight.Medium,
                fontSize = 20.sp,
                color = NeutralText,
            ),
        subtitle1 =
            TextStyle(
                fontFamily = displayFont,
                fontWeight = FontWeight.Medium,
                fontSize = 17.sp,
                color = NeutralText,
            ),
        body1 =
            TextStyle(
                fontFamily = bodyFont,
                fontWeight = FontWeight.Normal,
                fontSize = 15.sp,
                color = NeutralText,
            ),
        body2 =
            TextStyle(
                fontFamily = bodyFont,
                fontWeight = FontWeight.Normal,
                fontSize = 13.sp,
                color = NeutralTextMuted,
            ),
        button =
            TextStyle(
                fontFamily = bodyFont,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
            ),
        caption =
            TextStyle(
                fontFamily = bodyFont,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                color = NeutralTextMuted,
            ),
    )
}
