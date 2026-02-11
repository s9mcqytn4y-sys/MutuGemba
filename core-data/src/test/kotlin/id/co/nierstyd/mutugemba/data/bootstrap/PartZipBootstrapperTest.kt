package id.co.nierstyd.mutugemba.data.bootstrap

import id.co.nierstyd.mutugemba.data.local.assetstore.createDesktopHashAssetStore
import id.co.nierstyd.mutugemba.data.local.db.SqliteDatabase
import java.nio.file.Files
import java.nio.file.Paths
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
    @EnabledIf("extractedExists")
    fun bootstrapFromExtractedDirIfEmpty_skipsWhenPartTableAlreadyPopulated() {
        val tempDir = Files.createTempDirectory("mutugemba-bootstrap-test-second")
        val db = SqliteDatabase(tempDir.resolve("mapping.db"))
        val assetStore = createDesktopHashAssetStore(tempDir)
        val bootstrapper = PartZipBootstrapper(db, assetStore)

        val first = bootstrapper.bootstrapFromExtractedDirIfEmpty(defaultExtractedDir())
        val second = bootstrapper.bootstrapFromExtractedDirIfEmpty(defaultExtractedDir())

        assertNotNull(first)
        assertNull(second)
    }

    companion object {
        @JvmStatic
        fun extractedExists(): Boolean = Files.exists(defaultExtractedDir().resolve("mappings").resolve("mapping.json"))

        private fun defaultExtractedDir() =
            Paths.get(System.getProperty("user.dir"), "data", "part_assets", "extracted")
    }
}
