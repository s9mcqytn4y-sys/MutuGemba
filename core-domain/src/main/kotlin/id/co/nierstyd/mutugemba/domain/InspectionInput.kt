package id.co.nierstyd.mutugemba.domain

data class InspectionInput(
    val kind: InspectionKind,
    val lineId: Long,
    val shiftId: Long,
    val partId: Long,
    val totalCheck: Int? = null,
    val defectTypeId: Long?,
    val defectQuantity: Int?,
    val defects: List<InspectionDefectEntry> = emptyList(),
    val picName: String = "Admin QC",
    val createdAt: String,
)
