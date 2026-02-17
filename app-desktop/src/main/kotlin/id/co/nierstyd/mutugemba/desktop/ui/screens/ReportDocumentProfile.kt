package id.co.nierstyd.mutugemba.desktop.ui.screens

import java.awt.Color

enum class ReportColorProfile {
    PRINT,
    EXPORT,
}

data class ReportPalette(
    val header: Color,
    val stripe: Color,
    val subtotal: Color,
    val total: Color,
    val border: Color,
)

internal fun paletteFor(profile: ReportColorProfile): ReportPalette =
    when (profile) {
        // Profil PRINT dibuat high-contrast agar aman di printer kantor.
        ReportColorProfile.PRINT ->
            ReportPalette(
                header = Color(236, 238, 240),
                stripe = Color(246, 246, 246),
                subtotal = Color(234, 238, 246),
                total = Color(221, 230, 244),
                border = Color(122, 128, 135),
            )
        // Profil EXPORT mempertahankan palet RGB aplikasi untuk pembacaan layar.
        ReportColorProfile.EXPORT ->
            ReportPalette(
                header = Color(234, 241, 252),
                stripe = Color(247, 249, 252),
                subtotal = Color(230, 238, 252),
                total = Color(212, 226, 251),
                border = Color(176, 187, 205),
            )
    }
