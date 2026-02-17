package id.co.nierstyd.mutugemba.usecase

import id.co.nierstyd.mutugemba.domain.DefectType
import id.co.nierstyd.mutugemba.domain.Line
import id.co.nierstyd.mutugemba.domain.LineCode
import id.co.nierstyd.mutugemba.domain.MasterDataRepository
import id.co.nierstyd.mutugemba.domain.Part
import id.co.nierstyd.mutugemba.domain.Shift

class GetLinesUseCase(
    private val repository: MasterDataRepository,
) {
    fun execute(): List<Line> = repository.getLines()
}

class GetShiftsUseCase(
    private val repository: MasterDataRepository,
) {
    fun execute(): List<Shift> = repository.getShifts()
}

class GetPartsUseCase(
    private val repository: MasterDataRepository,
) {
    fun execute(): List<Part> = repository.getParts()
}

class GetDefectTypesUseCase(
    private val repository: MasterDataRepository,
) {
    fun execute(): List<DefectType> = repository.getDefectTypes()
}

class UpsertDefectTypeUseCase(
    private val repository: MasterDataRepository,
) {
    fun execute(
        name: String,
        lineCode: LineCode,
    ): DefectType = repository.upsertDefectType(name, lineCode)
}

class DeleteDefectTypeUseCase(
    private val repository: MasterDataRepository,
) {
    fun execute(
        defectTypeId: Long,
        lineCode: LineCode,
    ): Boolean = repository.deleteDefectType(defectTypeId, lineCode)
}
