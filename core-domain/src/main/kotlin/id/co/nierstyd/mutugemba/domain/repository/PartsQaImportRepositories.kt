package id.co.nierstyd.mutugemba.domain.repository

import id.co.nierstyd.mutugemba.domain.model.AssetKey
import id.co.nierstyd.mutugemba.domain.model.AssetRef
import id.co.nierstyd.mutugemba.domain.model.DefectHeatmapCell
import id.co.nierstyd.mutugemba.domain.model.ModelDefectTop
import id.co.nierstyd.mutugemba.domain.model.PartDetail
import id.co.nierstyd.mutugemba.domain.model.PartFilter
import id.co.nierstyd.mutugemba.domain.model.PartListItem
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
