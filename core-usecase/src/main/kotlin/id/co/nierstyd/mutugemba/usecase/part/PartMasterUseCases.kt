package id.co.nierstyd.mutugemba.usecase.part

import id.co.nierstyd.mutugemba.domain.model.DefectMaster
import id.co.nierstyd.mutugemba.domain.model.MaterialMaster
import id.co.nierstyd.mutugemba.domain.model.NgOriginType
import id.co.nierstyd.mutugemba.domain.model.PartMasterDetail
import id.co.nierstyd.mutugemba.domain.model.PartMasterListItem
import id.co.nierstyd.mutugemba.domain.model.SaveDefectMasterCommand
import id.co.nierstyd.mutugemba.domain.model.SaveMaterialMasterCommand
import id.co.nierstyd.mutugemba.domain.model.SavePartDefectAssignmentCommand
import id.co.nierstyd.mutugemba.domain.model.SavePartMasterCommand
import id.co.nierstyd.mutugemba.domain.model.SupplierMaster
import id.co.nierstyd.mutugemba.domain.repository.PartMasterRepository
import java.util.Locale

class ListPartMastersUseCase(
    private val repository: PartMasterRepository,
) {
    suspend fun execute(lineCode: String? = null): List<PartMasterListItem> = repository.listParts(lineCode)
}

class GetPartMasterDetailUseCase(
    private val repository: PartMasterRepository,
) {
    suspend fun execute(partId: Long): PartMasterDetail? = repository.getPartDetail(partId)
}

class SavePartMasterUseCase(
    private val repository: PartMasterRepository,
) {
    suspend fun execute(command: SavePartMasterCommand): Long = repository.savePart(command)
}

class DeletePartMasterUseCase(
    private val repository: PartMasterRepository,
) {
    suspend fun execute(partId: Long): Boolean = repository.deletePart(partId)
}

class ReplacePartMaterialsUseCase(
    private val repository: PartMasterRepository,
) {
    suspend fun execute(
        partId: Long,
        materialIdsInOrder: List<Long>,
    ) = repository.replacePartMaterials(partId, materialIdsInOrder)
}

class ReplacePartDefectsUseCase(
    private val repository: PartMasterRepository,
) {
    suspend fun execute(
        partId: Long,
        assignments: List<SavePartDefectAssignmentCommand>,
    ) = repository.replacePartDefects(partId, assignments)
}

class ListMaterialMastersUseCase(
    private val repository: PartMasterRepository,
) {
    suspend fun execute(): List<MaterialMaster> = repository.listMaterials()
}

class SaveMaterialMasterUseCase(
    private val repository: PartMasterRepository,
) {
    suspend fun execute(command: SaveMaterialMasterCommand): Long {
        val normalizedName = command.name.trim()
        val materialId = command.id
        val supplierId = command.supplierId
        require(normalizedName.isNotBlank()) { "Nama bahan wajib diisi." }
        require(materialId == null || materialId > 0L) { "ID bahan tidak valid." }
        require(supplierId == null || supplierId > 0L) { "ID pemasok tidak valid." }
        return repository.saveMaterial(
            command.copy(name = normalizedName),
        )
    }
}

class DeleteMaterialMasterUseCase(
    private val repository: PartMasterRepository,
) {
    suspend fun execute(materialId: Long): Boolean = repository.deleteMaterial(materialId)
}

class ListSupplierMastersUseCase(
    private val repository: PartMasterRepository,
) {
    suspend fun execute(): List<SupplierMaster> = repository.listSuppliers()
}

class SaveSupplierMasterUseCase(
    private val repository: PartMasterRepository,
) {
    suspend fun execute(
        id: Long?,
        name: String,
    ): Long {
        val normalizedName = name.trim()
        require(normalizedName.isNotBlank()) { "Nama pemasok wajib diisi." }
        require(id == null || id > 0L) { "ID pemasok tidak valid." }
        return repository.saveSupplier(id, normalizedName)
    }
}

class DeleteSupplierMasterUseCase(
    private val repository: PartMasterRepository,
) {
    suspend fun execute(supplierId: Long): Boolean = repository.deleteSupplier(supplierId)
}

class ListDefectMastersUseCase(
    private val repository: PartMasterRepository,
) {
    suspend fun execute(): List<DefectMaster> = repository.listDefects()
}

class SaveDefectMasterUseCase(
    private val repository: PartMasterRepository,
) {
    suspend fun execute(command: SaveDefectMasterCommand): Long {
        val normalizedName = command.name.trim()
        val defectId = command.id
        val normalizedLine =
            command.lineCode
                ?.trim()
                ?.lowercase(Locale.getDefault())
                ?.takeIf { it.isNotBlank() }

        require(normalizedName.isNotBlank()) { "Nama Jenis NG wajib diisi." }
        require(defectId == null || defectId > 0L) { "ID Jenis NG tidak valid." }
        require(command.originType == NgOriginType.MATERIAL || command.originType == NgOriginType.PROCESS) {
            "Asal NG tidak valid."
        }
        require(normalizedLine == null || normalizedLine == "press" || normalizedLine == "sewing") {
            "Line Jenis NG harus press atau sewing."
        }
        return repository.saveDefect(
            command.copy(
                name = normalizedName,
                lineCode = normalizedLine,
            ),
        )
    }
}

class DeleteDefectMasterUseCase(
    private val repository: PartMasterRepository,
) {
    suspend fun execute(defectId: Long): Boolean = repository.deleteDefect(defectId)
}
