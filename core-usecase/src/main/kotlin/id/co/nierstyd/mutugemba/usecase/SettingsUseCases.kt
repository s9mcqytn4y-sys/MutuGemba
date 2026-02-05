package id.co.nierstyd.mutugemba.usecase

import id.co.nierstyd.mutugemba.domain.AppSettingsKeys
import id.co.nierstyd.mutugemba.domain.SettingsRepository

class GetLastVisitedPageUseCase(
    private val settingsRepository: SettingsRepository,
) {
    fun execute(): String? = settingsRepository.getString(AppSettingsKeys.LAST_PAGE)
}

class SetLastVisitedPageUseCase(
    private val settingsRepository: SettingsRepository,
) {
    fun execute(pageKey: String) {
        settingsRepository.putString(AppSettingsKeys.LAST_PAGE, pageKey)
    }
}

class GetLastInspectionTypeUseCase(
    private val settingsRepository: SettingsRepository,
) {
    fun execute(): String? = settingsRepository.getString(AppSettingsKeys.LAST_INSPECTION_TYPE)
}

class SetLastInspectionTypeUseCase(
    private val settingsRepository: SettingsRepository,
) {
    fun execute(value: String) {
        settingsRepository.putString(AppSettingsKeys.LAST_INSPECTION_TYPE, value)
    }
}
