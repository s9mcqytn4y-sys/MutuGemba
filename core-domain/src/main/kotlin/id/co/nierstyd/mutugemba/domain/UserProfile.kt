package id.co.nierstyd.mutugemba.domain

data class UserProfile(
    val id: Long,
    val name: String,
    val position: String,
    val lineCode: LineCode,
    val role: UserRole,
    val isActive: Boolean,
)
