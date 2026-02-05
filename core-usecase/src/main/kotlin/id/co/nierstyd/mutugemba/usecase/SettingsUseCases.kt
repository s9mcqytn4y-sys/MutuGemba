package id.co.nierstyd.mutugemba.usecase

import id.co.nierstyd.mutugemba.domain.AppSettingsKeys
import id.co.nierstyd.mutugemba.domain.InspectionKind
import id.co.nierstyd.mutugemba.domain.SettingsRepository

data class InspectionDefaults(
    val lineId: Long?,
    val shiftId: Long?,
    val partId: Long?,
    val defectTypeId: Long?,
    val ctqParameterId: Long?,
    val kind: InspectionKind?,
)

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

class GetInspectionDefaultsUseCase(
    private val settingsRepository: SettingsRepository,
) {
    fun execute(): InspectionDefaults {
        val lineId = settingsRepository.getString(AppSettingsKeys.LAST_LINE_ID)?.toLongOrNull()
        val shiftId = settingsRepository.getString(AppSettingsKeys.LAST_SHIFT_ID)?.toLongOrNull()
        val partId = settingsRepository.getString(AppSettingsKeys.LAST_PART_ID)?.toLongOrNull()
        val defectTypeId = settingsRepository.getString(AppSettingsKeys.LAST_DEFECT_TYPE_ID)?.toLongOrNull()
        val ctqParameterId = settingsRepository.getString(AppSettingsKeys.LAST_CTQ_PARAMETER_ID)?.toLongOrNull()
        val kind =
            settingsRepository.getString(AppSettingsKeys.LAST_INSPECTION_TYPE)?.let {
                runCatching { InspectionKind.valueOf(it) }.getOrNull()
            }

        return InspectionDefaults(
            lineId = lineId,
            shiftId = shiftId,
            partId = partId,
            defectTypeId = defectTypeId,
            ctqParameterId = ctqParameterId,
            kind = kind,
        )
    }
}

class SaveInspectionDefaultsUseCase(
    private val settingsRepository: SettingsRepository,
) {
    fun execute(defaults: InspectionDefaults) {
        defaults.lineId?.let { settingsRepository.putString(AppSettingsKeys.LAST_LINE_ID, it.toString()) }
        defaults.shiftId?.let { settingsRepository.putString(AppSettingsKeys.LAST_SHIFT_ID, it.toString()) }
        defaults.partId?.let { settingsRepository.putString(AppSettingsKeys.LAST_PART_ID, it.toString()) }
        defaults.defectTypeId?.let {
            settingsRepository.putString(AppSettingsKeys.LAST_DEFECT_TYPE_ID, it.toString())
        }
        defaults.ctqParameterId?.let {
            settingsRepository.putString(AppSettingsKeys.LAST_CTQ_PARAMETER_ID, it.toString())
        }
        defaults.kind?.let { settingsRepository.putString(AppSettingsKeys.LAST_INSPECTION_TYPE, it.name) }
    }
}
