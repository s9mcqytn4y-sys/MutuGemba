package id.co.nierstyd.mutugemba.data.bootstrap

import id.co.nierstyd.mutugemba.data.local.assetstore.createDesktopHashAssetStore
import id.co.nierstyd.mutugemba.data.local.db.SqliteDatabase
import java.nio.file.Files
import java.nio.file.Paths
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIf

class PartZipBootstrapperTest {
    @Test
    @EnabledIf("extractedExists")
    fun bootstrapFromExtractedDirIfEmpty_importsRealPartAndImageData() {
        val tempDir = Files.createTempDirectory("mutugemba-bootstrap-extracted")
        val db = SqliteDatabase(tempDir.resolve("mapping.db"))
        val assetStore = createDesktopHashAssetStore(tempDir)
        val bootstrapper = PartZipBootstrapper(db, assetStore)

        val summary = bootstrapper.bootstrapFromExtractedDirIfEmpty(defaultExtractedDir())

        assertNotNull(summary)
        requireNotNull(summary)
        assertTrue(summary.importedParts > 0)
        assertTrue(summary.importedImages > 0)
    }

    @Test
    @EnabledIf("zipExists")
    fun bootstrapFromZipIfEmpty_importsRealPartAndImageData() {
        val tempDir = Files.createTempDirectory("mutugemba-bootstrap-test")
        val db = SqliteDatabase(tempDir.resolve("mapping.db"))
        val assetStore = createDesktopHashAssetStore(tempDir)
        val bootstrapper = PartZipBootstrapper(db, assetStore)

        val summary = bootstrapper.bootstrapFromZipIfEmpty(defaultZipPath())

        assertNotNull(summary)
        requireNotNull(summary)
        assertTrue(summary.importedParts > 0)
        assertTrue(summary.importedImages > 0)

        val partCount =
            db.read { connection ->
                connection.createStatement().use { statement ->
                    statement.executeQuery("SELECT COUNT(*) FROM part").use { rs ->
                        rs.next()
                        rs.getInt(1)
                    }
                }
            }
        val imageCount =
            db.read { connection ->
                connection.createStatement().use { statement ->
                    statement.executeQuery("SELECT COUNT(*) FROM image_asset").use { rs ->
                        rs.next()
                        rs.getInt(1)
                    }
                }
            }

        assertEquals(summary.importedParts, partCount)
        assertEquals(summary.importedImages, imageCount)

        val pngCount =
            Files
                .walk(tempDir.resolve("assets_store"))
                .use { stream ->
                    stream
                        .filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".png") }
                        .count()
                }
        assertTrue(pngCount > 0)
    }

    @Test
    @EnabledIf("zipExists")
    fun bootstrapFromZipIfEmpty_skipsWhenPartTableAlreadyPopulated() {
        val tempDir = Files.createTempDirectory("mutugemba-bootstrap-test-second")
        val db = SqliteDatabase(tempDir.resolve("mapping.db"))
        val assetStore = createDesktopHashAssetStore(tempDir)
        val bootstrapper = PartZipBootstrapper(db, assetStore)

        val first = bootstrapper.bootstrapFromZipIfEmpty(defaultZipPath())
        val second = bootstrapper.bootstrapFromZipIfEmpty(defaultZipPath())

        assertNotNull(first)
        assertNull(second)
    }

    companion object {
        @JvmStatic
        fun zipExists(): Boolean = Files.exists(defaultZipPath())

        @JvmStatic
        fun extractedExists(): Boolean = Files.exists(defaultExtractedDir().resolve("mappings").resolve("mapping.json"))

        private fun defaultZipPath() = Paths.get(System.getProperty("user.home"), "Downloads", "Part_Assets_Export.zip")

        private fun defaultExtractedDir() =
            Paths.get(System.getProperty("user.dir"), "data", "part_assets", "extracted")
    }
}
