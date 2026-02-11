package id.co.nierstyd.mutugemba.domain

interface MasterDataRepository {
    fun getLines(): List<Line>

    fun getShifts(): List<Shift>

    fun getParts(): List<Part>

    fun getDefectTypes(): List<DefectType>

    fun upsertDefectType(
        name: String,
        lineCode: LineCode,
    ): DefectType
}
