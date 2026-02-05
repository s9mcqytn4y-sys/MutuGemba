package id.co.nierstyd.mutugemba.domain

data class InspectionInput(
    val kind: InspectionKind,
    val lineId: Long,
    val shiftId: Long,
    val partId: Long,
    val defectTypeId: Long?,
    val defectQuantity: Int?,
    val ctqParameterId: Long?,
    val ctqValue: Double?,
    val createdAt: String,
)
