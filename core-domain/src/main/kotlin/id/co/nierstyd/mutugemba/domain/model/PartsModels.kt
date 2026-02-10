package id.co.nierstyd.mutugemba.domain.model

data class PartFilter(
    val lineCode: String? = null,
    val modelCode: String? = null,
    val search: String? = null,
    val year: Int? = null,
    val month: Int? = null,
    val limit: Int = 100,
    val offset: Int = 0,
)

data class PartListItem(
    val partId: Long,
    val uniqNo: String,
    val partNumber: String,
    val partName: String,
    val lineCode: String,
    val modelCodes: List<String>,
    val totalDefectMonthToDate: Int,
)

data class PartImage(
    val status: String,
    val storageRelPath: String,
    val sha256: String,
    val mime: String,
    val sizeBytes: Long,
    val widthPx: Int?,
    val heightPx: Int?,
)

data class PartMaterialLayer(
    val layerOrder: Int,
    val materialName: String,
    val weightG: Double?,
    val basisWeightGsm: Double?,
    val unit: String?,
)

data class PartRequirement(
    val modelCode: String,
    val qtyKbn: Int,
)

data class PartDetail(
    val partId: Long,
    val uniqNo: String,
    val partNumber: String,
    val partName: String,
    val lineCode: String,
    val models: List<String>,
    val image: PartImage?,
    val materials: List<PartMaterialLayer>,
    val requirements: List<PartRequirement>,
    val totalDefectMonthToDate: Int,
)
