package id.co.nierstyd.mutugemba.domain

data class InspectionRecord(
    val id: Long,
    val kind: InspectionKind,
    val lineName: String,
    val shiftName: String,
    val partName: String,
    val partNumber: String,
    val totalCheck: Int? = null,
    val createdAt: String,
)
