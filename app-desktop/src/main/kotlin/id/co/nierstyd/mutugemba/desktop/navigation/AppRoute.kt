package id.co.nierstyd.mutugemba.desktop.navigation

import id.co.nierstyd.mutugemba.desktop.ui.resources.AppStrings

enum class AppRoute(
    val key: String,
    val label: String,
) {
    Home("home", AppStrings.Navigation.Home),
    Inspection("inspection", AppStrings.Navigation.Inspection),
    Abnormal("abnormal", AppStrings.Navigation.Abnormal),
    Reports("reports", AppStrings.Navigation.Reports),
    ReportsMonthly("reports_monthly", AppStrings.Navigation.ReportsMonthly),
    Settings("settings", AppStrings.Navigation.Settings),
    ;

    companion object {
        fun fromKey(key: String?): AppRoute? = values().firstOrNull { it.key == key }
    }
}
