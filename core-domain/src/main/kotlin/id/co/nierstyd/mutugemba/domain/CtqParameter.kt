package id.co.nierstyd.mutugemba.domain

data class CtqParameter(
    val id: Long,
    val code: String,
    val name: String,
    val unit: String,
    val lowerLimit: Double?,
    val upperLimit: Double?,
    val targetValue: Double?,
)
