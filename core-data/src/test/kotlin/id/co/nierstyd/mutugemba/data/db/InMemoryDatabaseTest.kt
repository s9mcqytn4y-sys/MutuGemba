package id.co.nierstyd.mutugemba.data.db

import id.co.nierstyd.mutugemba.domain.LineCode
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

    @Test
    fun upsertDefectType_preventsDuplicateAndKeepsLineScope() {
        val db = InMemoryDatabase(Files.createTempFile("mutugemba-test", ".db"))

        val first = db.upsertDefectType("Kerutan Kritis", LineCode.PRESS)
        val second = db.upsertDefectType("KERUTAN KRITIS", LineCode.PRESS)

        assertEquals(first.id, second.id)
        assertEquals(LineCode.PRESS, first.lineCode)
        assertTrue(db.defectTypes.any { it.id == first.id && it.category == "CUSTOM" })
    }
}
