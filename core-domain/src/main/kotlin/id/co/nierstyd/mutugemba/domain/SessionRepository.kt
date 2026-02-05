package id.co.nierstyd.mutugemba.domain

interface SessionRepository {
    fun getActiveSession(): UserSession?

    fun saveSession(session: UserSession)

    fun clearSession()
}
