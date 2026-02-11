package id.co.nierstyd.mutugemba.domain

data class DefectType(
    val id: Long,
    val code: String,
    val name: String,
    val category: String,
    val severity: DefectSeverity,
    val lineCode: LineCode? = null,
)
