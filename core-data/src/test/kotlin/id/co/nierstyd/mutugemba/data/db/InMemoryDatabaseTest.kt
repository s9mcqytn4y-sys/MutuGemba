package id.co.nierstyd.mutugemba.data.db

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files

class InMemoryDatabaseTest {
    @Test
    fun defectTypes_and_partRecommendations_areDeduplicated() {
        val db = InMemoryDatabase(Files.createTempFile("mutugemba-test", ".db"))

        val defectCodes = db.defectTypes.map { it.code }
        assertEquals(defectCodes.distinct().size, defectCodes.size, "Defect code must be unique")

        val knownCodes = defectCodes.toSet()
        db.parts.forEach { part ->
            val recommended = part.recommendedDefectCodes
            assertEquals(
                recommended.distinct().size,
                recommended.size,
                "Recommended defect code must not duplicate for part ${part.uniqCode}",
            )
            assertTrue(
                recommended.all { it in knownCodes },
                "All recommended defect codes must exist in master defect type list",
            )
        }
    }
}

