package id.co.nierstyd.mutugemba.usecase

import id.co.nierstyd.mutugemba.domain.AppDataResetter
import id.co.nierstyd.mutugemba.domain.SessionRepository

class ResetDataUseCase(
    private val dataResetter: AppDataResetter,
    private val sessionRepository: SessionRepository,
) {
    fun execute(): Boolean {
        sessionRepository.clearSession()
        return dataResetter.resetAll()
    }
}
