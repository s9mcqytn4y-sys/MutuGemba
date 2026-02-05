package id.co.nierstyd.mutugemba.domain

enum class UserRole {
    USER,
    ADMIN,
    ;

    companion object {
        fun fromStorage(value: String?): UserRole = values().firstOrNull { it.name == value } ?: USER
    }
}
