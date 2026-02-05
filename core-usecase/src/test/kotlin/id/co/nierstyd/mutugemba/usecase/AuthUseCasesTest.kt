package id.co.nierstyd.mutugemba.usecase

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AuthUseCasesTest {
    @Test
    fun `hash and verify password`() {
        val salt = "abc123"
        val password = "rahasia"
        val hash = PasswordHasher.hash(password, salt)

        assertTrue(PasswordHasher.verify(password, salt, hash))
        assertFalse(PasswordHasher.verify("salah", salt, hash))
    }
}
