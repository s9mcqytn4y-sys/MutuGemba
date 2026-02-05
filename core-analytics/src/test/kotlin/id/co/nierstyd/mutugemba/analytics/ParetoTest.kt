package id.co.nierstyd.mutugemba.analytics

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ParetoTest {
    @Test
    fun `paretoCounts returns sorted counts desc`() {
        val items = listOf("Cat", "Dog", "Cat", "Bird", "Dog", "Cat")

        val result = paretoCounts(items)

        assertEquals(
            listOf(
                "Cat" to 3,
                "Dog" to 2,
                "Bird" to 1,
            ),
            result,
        )
    }
}
