package id.co.nierstyd.mutugemba.usecase

import id.co.nierstyd.mutugemba.domain.AppDataResetter
import id.co.nierstyd.mutugemba.domain.CacheInvalidator
import id.co.nierstyd.mutugemba.domain.SessionRepository

class ResetDataUseCase(
    private val dataResetter: AppDataResetter,
    private val sessionRepository: SessionRepository,
    private val cacheInvalidators: List<CacheInvalidator> = emptyList(),
) {
    fun execute(): Boolean {
        sessionRepository.clearSession()
        val result = dataResetter.resetAll()
        if (result) {
            cacheInvalidators.forEach { it.clearCache() }
        }
        return result
    }
}
