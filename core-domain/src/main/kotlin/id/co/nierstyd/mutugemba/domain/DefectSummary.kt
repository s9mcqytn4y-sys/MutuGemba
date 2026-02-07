package id.co.nierstyd.mutugemba.domain

data class DefectSummary(
    val defectTypeId: Long,
    val defectName: String,
    val category: String,
    val severity: DefectSeverity,
    val totalQuantity: Int,
)
