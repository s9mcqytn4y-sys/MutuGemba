package id.co.nierstyd.mutugemba.desktop.ui.util

import id.co.nierstyd.mutugemba.domain.InspectionKind

fun InspectionKind.toDisplayLabel(): String =
    when (this) {
        InspectionKind.DEFECT -> "Cacat"
        InspectionKind.CTQ -> "Parameter Proses"
    }
