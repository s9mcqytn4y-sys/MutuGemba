package id.co.nierstyd.mutugemba.domain

interface UserPreferencesRepository {
    fun getPreference(
        userId: Long,
        key: String,
    ): String?

    fun setPreference(
        userId: Long,
        key: String,
        value: String,
    )

    fun getAll(userId: Long): Map<String, String>
}
