package id.co.nierstyd.mutugemba.usecase.part

import id.co.nierstyd.mutugemba.domain.model.PartDetail
import id.co.nierstyd.mutugemba.domain.model.PartFilter
import id.co.nierstyd.mutugemba.domain.model.PartListItem
import id.co.nierstyd.mutugemba.domain.repository.PartRepository
import kotlinx.coroutines.flow.Flow

class ObservePartsUseCase(
    private val repository: PartRepository,
) {
    fun execute(filter: PartFilter): Flow<List<PartListItem>> = repository.observeParts(filter)
}

class GetPartDetailUseCase(
    private val repository: PartRepository,
) {
    suspend fun execute(
        uniqNo: String,
        year: Int,
        month: Int,
    ): PartDetail? = repository.getPartDetail(uniqNo, year, month)
}
