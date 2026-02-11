package id.co.nierstyd.mutugemba.data

import id.co.nierstyd.mutugemba.domain.LineCode
import id.co.nierstyd.mutugemba.domain.SessionRepository
import id.co.nierstyd.mutugemba.domain.UserRole
import id.co.nierstyd.mutugemba.domain.UserSession

class SettingsSessionRepository(
    private val settingsRepository: FileSettingsRepository,
) : SessionRepository {
    override fun getActiveSession(): UserSession? {
        val userId = settingsRepository.getString(KEY_USER_ID)?.toLongOrNull() ?: return null
        val expiresAt = settingsRepository.getString(KEY_EXPIRES_AT)?.toLongOrNull() ?: return null
        val session =
            UserSession(
                userId = userId,
                name = settingsRepository.getString(KEY_NAME) ?: "Operator QC",
                employeeId = settingsRepository.getString(KEY_EMPLOYEE_ID),
                fullName = settingsRepository.getString(KEY_FULL_NAME),
                position = settingsRepository.getString(KEY_POSITION) ?: "QC",
                department = settingsRepository.getString(KEY_DEPARTMENT),
                lineCode = LineCode.fromStorage(settingsRepository.getString(KEY_LINE_CODE)),
                role =
                    runCatching {
                        UserRole.valueOf(settingsRepository.getString(KEY_ROLE) ?: UserRole.USER.name)
                    }.getOrDefault(UserRole.USER),
                loginAtEpochMs =
                    settingsRepository.getString(KEY_LOGIN_AT)?.toLongOrNull()
                        ?: System.currentTimeMillis(),
                expiresAtEpochMs = expiresAt,
                photoPath = settingsRepository.getString(KEY_PHOTO_PATH),
            )
        return session.takeIf { it.isActive }
    }

    override fun saveSession(session: UserSession) {
        settingsRepository.putString(KEY_USER_ID, session.userId.toString())
        settingsRepository.putString(KEY_NAME, session.name)
        settingsRepository.putString(KEY_EMPLOYEE_ID, session.employeeId.orEmpty())
        settingsRepository.putString(KEY_FULL_NAME, session.fullName.orEmpty())
        settingsRepository.putString(KEY_POSITION, session.position)
        settingsRepository.putString(KEY_DEPARTMENT, session.department.orEmpty())
        settingsRepository.putString(KEY_LINE_CODE, session.lineCode.name)
        settingsRepository.putString(KEY_ROLE, session.role.name)
        settingsRepository.putString(KEY_LOGIN_AT, session.loginAtEpochMs.toString())
        settingsRepository.putString(KEY_EXPIRES_AT, session.expiresAtEpochMs.toString())
        settingsRepository.putString(KEY_PHOTO_PATH, session.photoPath.orEmpty())
    }

    override fun clearSession() {
        settingsRepository.putString(KEY_USER_ID, "")
        settingsRepository.putString(KEY_NAME, "")
        settingsRepository.putString(KEY_EMPLOYEE_ID, "")
        settingsRepository.putString(KEY_FULL_NAME, "")
        settingsRepository.putString(KEY_POSITION, "")
        settingsRepository.putString(KEY_DEPARTMENT, "")
        settingsRepository.putString(KEY_LINE_CODE, "")
        settingsRepository.putString(KEY_ROLE, "")
        settingsRepository.putString(KEY_LOGIN_AT, "")
        settingsRepository.putString(KEY_EXPIRES_AT, "")
        settingsRepository.putString(KEY_PHOTO_PATH, "")
    }

    private companion object {
        const val KEY_USER_ID = "session.userId"
        const val KEY_NAME = "session.name"
        const val KEY_EMPLOYEE_ID = "session.employeeId"
        const val KEY_FULL_NAME = "session.fullName"
        const val KEY_POSITION = "session.position"
        const val KEY_DEPARTMENT = "session.department"
        const val KEY_LINE_CODE = "session.lineCode"
        const val KEY_ROLE = "session.role"
        const val KEY_LOGIN_AT = "session.loginAt"
        const val KEY_EXPIRES_AT = "session.expiresAt"
        const val KEY_PHOTO_PATH = "session.photoPath"
    }
}
