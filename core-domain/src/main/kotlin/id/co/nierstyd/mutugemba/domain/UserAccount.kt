package id.co.nierstyd.mutugemba.domain

data class UserAccount(
    val id: Long,
    val name: String,
    val passwordHash: String,
    val passwordSalt: String,
    val employeeId: String? = null,
    val fullName: String? = null,
    val position: String,
    val department: String? = null,
    val lineCode: LineCode,
    val role: UserRole,
    val isActive: Boolean,
    val photoPath: String? = null,
)
