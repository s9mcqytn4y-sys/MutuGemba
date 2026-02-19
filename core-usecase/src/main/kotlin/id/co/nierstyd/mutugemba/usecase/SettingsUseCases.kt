package id.co.nierstyd.mutugemba.usecase

import id.co.nierstyd.mutugemba.domain.AppSettingsKeys
import id.co.nierstyd.mutugemba.domain.SettingsRepository
import java.time.LocalDate
import java.time.format.DateTimeParseException

data class InspectionDefaults(
    val lineId: Long?,
    val qcLineId: Long?,
    val shiftId: Long?,
    val partId: Long?,
    val defectTypeId: Long?,
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
        val qcLineId = settingsRepository.getString(AppSettingsKeys.DEV_QC_LINE_ID)?.toLongOrNull()
        val lastLineId = settingsRepository.getString(AppSettingsKeys.LAST_LINE_ID)?.toLongOrNull()
        val lineId = qcLineId ?: lastLineId
        val shiftId = settingsRepository.getString(AppSettingsKeys.LAST_SHIFT_ID)?.toLongOrNull()
        val partId = settingsRepository.getString(AppSettingsKeys.LAST_PART_ID)?.toLongOrNull()
        val defectTypeId = settingsRepository.getString(AppSettingsKeys.LAST_DEFECT_TYPE_ID)?.toLongOrNull()

        return InspectionDefaults(
            lineId = lineId,
            qcLineId = qcLineId,
            shiftId = shiftId,
            partId = partId,
            defectTypeId = defectTypeId,
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
    }
}

class GetDevQcLineUseCase(
    private val settingsRepository: SettingsRepository,
) {
    fun execute(): Long? =
        settingsRepository
            .getString(AppSettingsKeys.DEV_QC_LINE_ID)
            ?.toLongOrNull()
}

class SetDevQcLineUseCase(
    private val settingsRepository: SettingsRepository,
) {
    fun execute(lineId: Long?) {
        settingsRepository.putString(AppSettingsKeys.DEV_QC_LINE_ID, lineId?.toString().orEmpty())
    }
}

class GetDevDemoModeUseCase(
    private val settingsRepository: SettingsRepository,
) {
    fun execute(): Boolean =
        settingsRepository
            .getString(AppSettingsKeys.DEV_DEMO_MODE)
            ?.toBooleanStrictOrNull()
            ?: false
}

class SetDevDemoModeUseCase(
    private val settingsRepository: SettingsRepository,
) {
    fun execute(enabled: Boolean) {
        settingsRepository.putString(AppSettingsKeys.DEV_DEMO_MODE, enabled.toString())
    }
}

class GetDevDummyDataUseCase(
    private val settingsRepository: SettingsRepository,
) {
    fun execute(): Boolean =
        settingsRepository
            .getString(AppSettingsKeys.DEV_USE_DUMMY_DATA)
            ?.toBooleanStrictOrNull()
            ?: false
}

class SetDevDummyDataUseCase(
    private val settingsRepository: SettingsRepository,
) {
    fun execute(enabled: Boolean) {
        settingsRepository.putString(AppSettingsKeys.DEV_USE_DUMMY_DATA, enabled.toString())
    }
}

class GetAllowDuplicateInspectionUseCase(
    private val settingsRepository: SettingsRepository,
) {
    fun execute(): Boolean =
        settingsRepository
            .getString(AppSettingsKeys.ALLOW_DUPLICATE_INSPECTION)
            ?.toBooleanStrictOrNull()
            ?: false
}

class SetAllowDuplicateInspectionUseCase(
    private val settingsRepository: SettingsRepository,
) {
    fun execute(allow: Boolean) {
        settingsRepository.putString(AppSettingsKeys.ALLOW_DUPLICATE_INSPECTION, allow.toString())
    }
}

class GetManualHolidayDatesUseCase(
    private val settingsRepository: SettingsRepository,
) {
    fun execute(): Set<LocalDate> {
        val raw = settingsRepository.getString(AppSettingsKeys.MANUAL_HOLIDAY_DATES).orEmpty()
        if (raw.isBlank()) return emptySet()
        return raw
            .split("|")
            .mapNotNull {
                try {
                    LocalDate.parse(it)
                } catch (_: DateTimeParseException) {
                    null
                }
            }.toSet()
    }
}

class SaveManualHolidayDatesUseCase(
    private val settingsRepository: SettingsRepository,
) {
    fun execute(dates: Set<LocalDate>) {
        val value = dates.sorted().joinToString("|") { it.toString() }
        settingsRepository.putString(AppSettingsKeys.MANUAL_HOLIDAY_DATES, value)
    }
}

class GetInspectionPartDefectLayoutUseCase(
    private val settingsRepository: SettingsRepository,
) {
    fun execute(): Map<Long, List<Long>> {
        val raw = settingsRepository.getString(AppSettingsKeys.INSPECTION_PART_DEFECT_LAYOUT).orEmpty()
        if (raw.isBlank()) return emptyMap()
        return raw
            .split("|")
            .mapNotNull { chunk ->
                val parts = chunk.split("=")
                if (parts.size != 2) return@mapNotNull null
                val partId = parts[0].toLongOrNull() ?: return@mapNotNull null
                val defectIds =
                    parts[1]
                        .split(",")
                        .mapNotNull { it.toLongOrNull() }
                        .distinct()
                if (defectIds.isEmpty()) return@mapNotNull null
                partId to defectIds
            }.toMap()
    }
}

class SaveInspectionPartDefectLayoutUseCase(
    private val settingsRepository: SettingsRepository,
) {
    fun execute(overrides: Map<Long, List<Long>>) {
        val value =
            overrides
                .toSortedMap()
                .mapNotNull { (partId, defectIds) ->
                    val ordered = defectIds.filter { it > 0 }.distinct()
                    if (ordered.isEmpty()) {
                        null
                    } else {
                        "$partId=${ordered.joinToString(",")}"
                    }
                }.joinToString("|")
        settingsRepository.putString(AppSettingsKeys.INSPECTION_PART_DEFECT_LAYOUT, value)
    }
}
