package id.co.nierstyd.mutugemba.domain

interface SettingsRepository {
    fun getString(key: String): String?

    fun putString(
        key: String,
        value: String,
    )
}

object AppSettingsKeys {
    const val LAST_PAGE = "nav.lastPage"
    const val LAST_INSPECTION_TYPE = "input.lastInspectionType"
    const val LAST_LINE_ID = "input.lastLineId"
    const val LAST_SHIFT_ID = "input.lastShiftId"
    const val LAST_PART_ID = "input.lastPartId"
    const val LAST_DEFECT_TYPE_ID = "input.lastDefectTypeId"
    const val LAST_CTQ_PARAMETER_ID = "input.lastCtqParameterId"
}
