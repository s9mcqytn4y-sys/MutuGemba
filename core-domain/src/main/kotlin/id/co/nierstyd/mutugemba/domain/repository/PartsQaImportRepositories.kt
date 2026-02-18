package id.co.nierstyd.mutugemba.domain.repository

import id.co.nierstyd.mutugemba.domain.model.AssetKey
import id.co.nierstyd.mutugemba.domain.model.AssetRef
import id.co.nierstyd.mutugemba.domain.model.DefectHeatmapCell
import id.co.nierstyd.mutugemba.domain.model.DefectMaster
import id.co.nierstyd.mutugemba.domain.model.MaterialMaster
import id.co.nierstyd.mutugemba.domain.model.ModelDefectTop
import id.co.nierstyd.mutugemba.domain.model.PartDetail
import id.co.nierstyd.mutugemba.domain.model.PartFilter
import id.co.nierstyd.mutugemba.domain.model.PartListItem
import id.co.nierstyd.mutugemba.domain.model.PartMasterDetail
import id.co.nierstyd.mutugemba.domain.model.PartMasterListItem
import id.co.nierstyd.mutugemba.domain.model.SaveDefectMasterCommand
import id.co.nierstyd.mutugemba.domain.model.SaveMaterialMasterCommand
import id.co.nierstyd.mutugemba.domain.model.SavePartDefectAssignmentCommand
import id.co.nierstyd.mutugemba.domain.model.SavePartMasterCommand
import id.co.nierstyd.mutugemba.domain.model.SupplierMaster
import kotlinx.coroutines.flow.Flow
import java.io.InputStream

interface PartRepository {
    fun observeParts(filter: PartFilter): Flow<List<PartListItem>>

    suspend fun getPartDetail(
        uniqNo: String,
        year: Int,
        month: Int,
    ): PartDetail?
}

interface PartMasterRepository {
    suspend fun listParts(lineCode: String? = null): List<PartMasterListItem>

    suspend fun getPartDetail(partId: Long): PartMasterDetail?

    suspend fun savePart(command: SavePartMasterCommand): Long

    suspend fun deletePart(partId: Long): Boolean

    suspend fun replacePartMaterials(
        partId: Long,
        materialIdsInOrder: List<Long>,
    )

    suspend fun replacePartDefects(
        partId: Long,
        assignments: List<SavePartDefectAssignmentCommand>,
    )

    suspend fun listMaterials(): List<MaterialMaster>

    suspend fun saveMaterial(command: SaveMaterialMasterCommand): Long

    suspend fun deleteMaterial(materialId: Long): Boolean

    suspend fun listSuppliers(): List<SupplierMaster>

    suspend fun saveSupplier(
        id: Long?,
        name: String,
    ): Long

    suspend fun deleteSupplier(supplierId: Long): Boolean

    suspend fun listDefects(): List<DefectMaster>

    suspend fun saveDefect(command: SaveDefectMasterCommand): Long

    suspend fun deleteDefect(defectId: Long): Boolean
}

interface QARepository {
    suspend fun getTopDefectsPerModelMonthly(
        year: Int,
        month: Int,
        topN: Int,
    ): List<ModelDefectTop>

    suspend fun getDefectHeatmap(
        year: Int,
        month: Int,
        modelCode: String?,
    ): List<DefectHeatmapCell>
}

interface AssetStore {
    suspend fun putBytes(
        key: AssetKey,
        bytes: ByteArray,
        mime: String,
    ): AssetRef

    suspend fun getBytes(ref: AssetRef): ByteArray?

    suspend fun exists(ref: AssetRef): Boolean

    suspend fun openStream(ref: AssetRef): InputStream?

    suspend fun delete(ref: AssetRef): Boolean
}

interface AssetRepository {
    suspend fun getActiveImageRef(uniqNo: String): AssetRef?

    suspend fun loadImageBytes(ref: AssetRef): ByteArray?
}
