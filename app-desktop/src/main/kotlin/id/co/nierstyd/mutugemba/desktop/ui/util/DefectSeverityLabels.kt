package id.co.nierstyd.mutugemba.desktop.ui.util

import id.co.nierstyd.mutugemba.desktop.ui.resources.AppStrings
import id.co.nierstyd.mutugemba.domain.DefectSeverity

fun DefectSeverity.toDisplayLabel(): String =
    when (this) {
        DefectSeverity.NORMAL -> AppStrings.DefectSeverity.Normal
        DefectSeverity.KRITIS -> AppStrings.DefectSeverity.Critical
    }
