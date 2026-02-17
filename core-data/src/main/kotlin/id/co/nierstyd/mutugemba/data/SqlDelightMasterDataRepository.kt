package id.co.nierstyd.mutugemba.data

import id.co.nierstyd.mutugemba.data.db.InMemoryDatabase
import id.co.nierstyd.mutugemba.domain.CacheInvalidator
import id.co.nierstyd.mutugemba.domain.DefectType
import id.co.nierstyd.mutugemba.domain.Line
import id.co.nierstyd.mutugemba.domain.LineCode
import id.co.nierstyd.mutugemba.domain.MasterDataRepository
import id.co.nierstyd.mutugemba.domain.Part
import id.co.nierstyd.mutugemba.domain.Shift

class SqlDelightMasterDataRepository(
    private val database: InMemoryDatabase,
) : MasterDataRepository,
    CacheInvalidator {
    override fun getLines(): List<Line> = database.lines

    override fun getShifts(): List<Shift> = database.shifts

    override fun getParts(): List<Part> = database.parts

    override fun getDefectTypes(): List<DefectType> = database.defectTypes

    override fun upsertDefectType(
        name: String,
        lineCode: LineCode,
    ): DefectType = database.upsertDefectType(name, lineCode)

    override fun deleteDefectType(
        defectTypeId: Long,
        lineCode: LineCode,
    ): Boolean = database.deleteDefectType(defectTypeId, lineCode)

    override fun clearCache() {
        // In-memory data is static for current fallback implementation.
    }
}
