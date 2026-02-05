package id.co.nierstyd.mutugemba.domain

data class Part(
    val id: Long,
    val partNumber: String,
    val model: String,
    val name: String,
    val uniqCode: String,
    val material: String,
    val picturePath: String?,
    val lineCode: LineCode,
)
