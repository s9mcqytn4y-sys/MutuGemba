package id.co.nierstyd.mutugemba.data.db

import id.co.nierstyd.mutugemba.domain.LineCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
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

    @Test
    fun upsertDefectType_normalizesExcelAliasNames() {
        val db = InMemoryDatabase(Files.createTempFile("mutugemba-test", ".db"))

        val first = db.upsertDefectType("SPUNBOUND TDK MEREKAT", LineCode.SEWING)
        val second = db.upsertDefectType("SPOUNDBOUND TIDAK MEREKAT", LineCode.SEWING)

        assertEquals(first.id, second.id)
    }

    @Test
    fun customDefect_isPersistedAcrossAppRestart() {
        val dbFile = Files.createTempFile("mutugemba-custom-defect", ".db")
        val first = InMemoryDatabase(dbFile)
        val created = first.upsertDefectType("KERUTAN SISI KANAN", LineCode.PRESS)

        val reloaded = InMemoryDatabase(dbFile)
        assertTrue(reloaded.defectTypes.any { it.id == created.id && it.category == "CUSTOM" })
    }

    @Test
    fun deleteDefectType_removesPersistedCustomDefect() {
        val dbFile = Files.createTempFile("mutugemba-delete-defect", ".db")
        val first = InMemoryDatabase(dbFile)
        val created = first.upsertDefectType("GORESAN OVAL", LineCode.SEWING)

        val deleted = first.deleteDefectType(created.id, LineCode.SEWING)
        assertTrue(deleted)
        assertFalse(first.defectTypes.any { it.id == created.id })

        val reloaded = InMemoryDatabase(dbFile)
        assertFalse(reloaded.defectTypes.any { it.id == created.id })
    }
}
