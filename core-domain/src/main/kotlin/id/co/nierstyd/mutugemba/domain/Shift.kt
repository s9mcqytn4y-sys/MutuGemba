package id.co.nierstyd.mutugemba.domain

data class Shift(
    val id: Long,
    val code: String,
    val name: String,
    val startTime: String?,
    val endTime: String?,
)
