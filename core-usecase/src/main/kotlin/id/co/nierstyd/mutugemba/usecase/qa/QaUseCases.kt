package id.co.nierstyd.mutugemba.usecase.qa

import id.co.nierstyd.mutugemba.domain.model.DefectHeatmapCell
import id.co.nierstyd.mutugemba.domain.model.ModelDefectTop
import id.co.nierstyd.mutugemba.domain.repository.QARepository

class GetTopDefectsPerModelMonthlyUseCase(
    private val repository: QARepository,
) {
    suspend fun execute(
        year: Int,
        month: Int,
        topN: Int,
    ): List<ModelDefectTop> = repository.getTopDefectsPerModelMonthly(year, month, topN)
}

class GetDefectHeatmapUseCase(
    private val repository: QARepository,
) {
    suspend fun execute(
        year: Int,
        month: Int,
        modelCode: String?,
    ): List<DefectHeatmapCell> = repository.getDefectHeatmap(year, month, modelCode)
}
