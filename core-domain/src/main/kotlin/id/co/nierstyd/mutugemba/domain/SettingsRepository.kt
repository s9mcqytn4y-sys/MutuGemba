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
}
