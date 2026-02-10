package id.co.nierstyd.mutugemba.domain.model

data class ModelDefectTop(
    val modelCode: String,
    val periodYear: Int,
    val periodMonth: Int,
    val defectTypeId: String,
    val defectName: String,
    val totalQty: Int,
)

data class DefectHeatmapCell(
    val reportDate: String?,
    val defectTypeId: String,
    val defectName: String,
    val totalQty: Int,
)
