package id.co.nierstyd.mutugemba.domain

data class UserSession(
    val userId: Long,
    val name: String,
    val employeeId: String? = null,
    val fullName: String? = null,
    val position: String,
    val department: String? = null,
    val lineCode: LineCode,
    val role: UserRole,
    val loginAtEpochMs: Long,
    val expiresAtEpochMs: Long,
    val photoPath: String? = null,
) {
    val isActive: Boolean
        get() = System.currentTimeMillis() < expiresAtEpochMs
}
