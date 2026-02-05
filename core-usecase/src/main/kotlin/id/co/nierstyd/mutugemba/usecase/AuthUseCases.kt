package id.co.nierstyd.mutugemba.usecase

import id.co.nierstyd.mutugemba.domain.SessionRepository
import id.co.nierstyd.mutugemba.domain.UserRepository
import id.co.nierstyd.mutugemba.domain.UserRole
import id.co.nierstyd.mutugemba.domain.UserSession
import java.security.MessageDigest

data class LoginResult(
    val session: UserSession?,
    val feedback: UserFeedback,
)

class LoginUseCase(
    private val userRepository: UserRepository,
    private val sessionRepository: SessionRepository,
    private val sessionDurationHours: Long = 8,
) {
    fun execute(
        name: String,
        password: String,
    ): LoginResult {
        val normalizedName = name.trim()
        val user =
            if (normalizedName.isBlank() || password.isBlank()) {
                null
            } else {
                userRepository.findByName(normalizedName)
            }
        val feedback =
            when {
                normalizedName.isBlank() || password.isBlank() ->
                    UserFeedback(FeedbackType.ERROR, "Nama dan password wajib diisi.")
                user == null -> UserFeedback(FeedbackType.ERROR, "User tidak ditemukan.")
                !user.isActive -> UserFeedback(FeedbackType.ERROR, "User non-aktif. Hubungi admin.")
                !PasswordHasher.verify(password, user.passwordSalt, user.passwordHash) ->
                    UserFeedback(FeedbackType.ERROR, "Password salah.")
                else -> null
            }
        if (feedback != null || user == null) {
            return LoginResult(session = null, feedback = feedback ?: UserFeedback(FeedbackType.ERROR, "Login gagal."))
        }

        val now = System.currentTimeMillis()
        val session =
            UserSession(
                userId = user.id,
                name = user.name,
                position = user.position,
                lineCode = user.lineCode,
                role = user.role,
                loginAtEpochMs = now,
                expiresAtEpochMs = now + sessionDurationHours * 60 * 60 * 1000,
            )
        sessionRepository.saveSession(session)
        return LoginResult(
            session = session,
            feedback = UserFeedback(FeedbackType.SUCCESS, "Login berhasil."),
        )
    }
}

class GetActiveSessionUseCase(
    private val sessionRepository: SessionRepository,
) {
    fun execute(): UserSession? = sessionRepository.getActiveSession()
}

class LogoutUseCase(
    private val sessionRepository: SessionRepository,
) {
    fun execute() {
        sessionRepository.clearSession()
    }
}

object PasswordHasher {
    fun hash(
        password: String,
        salt: String,
    ): String {
        val bytes = (salt + password).toByteArray(Charsets.UTF_8)
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun verify(
        password: String,
        salt: String,
        expectedHash: String,
    ): Boolean = hash(password, salt) == expectedHash
}

fun UserRole.toDisplayLabel(): String =
    when (this) {
        UserRole.ADMIN -> "Admin"
        UserRole.USER -> "User"
    }
