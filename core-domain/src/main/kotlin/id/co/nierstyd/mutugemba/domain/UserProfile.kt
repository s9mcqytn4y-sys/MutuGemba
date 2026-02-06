package id.co.nierstyd.mutugemba.domain

data class UserProfile(
    val id: Long,
    val name: String,
    val employeeId: String? = null,
    val fullName: String? = null,
    val position: String,
    val department: String? = null,
    val lineCode: LineCode,
    val role: UserRole,
    val isActive: Boolean,
    val photoPath: String? = null,
)
