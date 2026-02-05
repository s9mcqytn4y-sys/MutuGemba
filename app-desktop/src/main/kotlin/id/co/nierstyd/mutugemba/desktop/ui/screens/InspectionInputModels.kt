package id.co.nierstyd.mutugemba.desktop.ui.screens

import id.co.nierstyd.mutugemba.domain.InspectionTimeSlot

internal data class PartDefectSlotKey(
    val partId: Long,
    val defectTypeId: Long,
    val slot: InspectionTimeSlot,
)

internal data class PartTotals(
    val totalCheck: Int,
    val totalDefect: Int,
    val totalOk: Int,
)

internal data class PartSummaryRow(
    val partNumber: String,
    val partName: String,
    val totalCheck: Int,
    val totalDefect: Int,
    val totalOk: Int,
)

internal data class SummaryTotals(
    val totalCheck: Int,
    val totalDefect: Int,
    val totalOk: Int,
    val ngRatio: Double,
)
