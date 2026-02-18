package id.co.nierstyd.mutugemba.domain.model

enum class NgOriginType {
    MATERIAL,
    PROCESS,
}

data class SupplierMaster(
    val id: Long,
    val name: String,
    val isActive: Boolean,
)

data class MaterialMaster(
    val id: Long,
    val name: String,
    val supplierId: Long?,
    val supplierName: String?,
    val clientSupplied: Boolean,
)

data class DefectMaster(
    val id: Long,
    val name: String,
    val originType: NgOriginType,
    val lineCode: String?,
)

data class PartMasterListItem(
    val id: Long,
    val uniqNo: String,
    val partNumber: String,
    val partName: String,
    val lineCode: String,
    val excludedFromChecksheet: Boolean,
)

data class PartMaterialAssignment(
    val materialId: Long,
    val materialName: String,
    val supplierName: String?,
    val layerOrder: Int,
)

data class PartDefectAssignment(
    val defectId: Long,
    val defectName: String,
    val originType: NgOriginType,
    val materialId: Long?,
    val materialName: String?,
)

data class PartMasterDetail(
    val id: Long,
    val uniqNo: String,
    val partNumber: String,
    val partName: String,
    val lineCode: String,
    val excludedFromChecksheet: Boolean,
    val materials: List<PartMaterialAssignment>,
    val defects: List<PartDefectAssignment>,
)

data class SavePartMasterCommand(
    val id: Long?,
    val uniqNo: String,
    val partNumber: String,
    val partName: String,
    val lineCode: String,
    val excludedFromChecksheet: Boolean,
)

data class SaveMaterialMasterCommand(
    val id: Long?,
    val name: String,
    val supplierId: Long?,
    val clientSupplied: Boolean,
)

data class SaveDefectMasterCommand(
    val id: Long?,
    val name: String,
    val originType: NgOriginType,
    val lineCode: String?,
)

data class SavePartDefectAssignmentCommand(
    val defectId: Long,
    val originType: NgOriginType,
    val materialId: Long?,
)
