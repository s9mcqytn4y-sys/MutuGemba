package id.co.nierstyd.mutugemba.domain

data class UserAccount(
    val id: Long,
    val name: String,
    val passwordHash: String,
    val passwordSalt: String,
    val position: String,
    val lineCode: LineCode,
    val role: UserRole,
    val isActive: Boolean,
)
