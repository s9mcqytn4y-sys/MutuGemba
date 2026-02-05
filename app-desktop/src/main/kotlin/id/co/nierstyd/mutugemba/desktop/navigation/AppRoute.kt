package id.co.nierstyd.mutugemba.desktop.navigation

enum class AppRoute(
    val key: String,
    val label: String,
) {
    Home("home", "Beranda"),
    Inspection("inspection", "Input Inspeksi"),
    Abnormal("abnormal", "Tiket Abnormal"),
    Reports("reports", "Laporan"),
    Settings("settings", "Pengaturan"),
    ;

    companion object {
        fun fromKey(key: String?): AppRoute? = values().firstOrNull { it.key == key }
    }
}
