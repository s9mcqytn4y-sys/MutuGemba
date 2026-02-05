package id.co.nierstyd.mutugemba.domain

data class UserSession(
    val userId: Long,
    val name: String,
    val position: String,
    val lineCode: LineCode,
    val role: UserRole,
    val loginAtEpochMs: Long,
    val expiresAtEpochMs: Long,
) {
    val isActive: Boolean
        get() = System.currentTimeMillis() < expiresAtEpochMs
}
