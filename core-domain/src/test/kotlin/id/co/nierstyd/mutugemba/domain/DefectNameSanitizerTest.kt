package id.co.nierstyd.mutugemba.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DefectNameSanitizerTest {
    @Test
    fun `normalize display removes prefix and trailing variant marker`() {
        val result = DefectNameSanitizer.normalizeDisplay("(CB9) OVERCUTTING J")
        assertEquals("OVERCUTTING", result)
    }

    @Test
    fun `expand problem items splits comma separated values`() {
        val result = DefectNameSanitizer.expandProblemItems("SPUNBOUND TERLIPAT, SPUNBOND HARDEN")
        assertEquals(listOf("SPUNBOUND TERLIPAT", "SPUNBOND HARDEN"), result)
    }
}
