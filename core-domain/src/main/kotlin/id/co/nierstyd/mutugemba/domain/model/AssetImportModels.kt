package id.co.nierstyd.mutugemba.domain.model

data class AssetKey(
    val type: String = "part_image",
    val uniqNo: String,
    val sha256: String,
)

data class AssetRef(
    val storageRelPath: String,
    val sha256: String,
    val mime: String,
    val sizeBytes: Long,
)
