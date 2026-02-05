package id.co.nierstyd.mutugemba.domain

interface UserRepository {
    fun findByName(name: String): UserAccount?

    fun getProfileById(id: Long): UserProfile?
}
