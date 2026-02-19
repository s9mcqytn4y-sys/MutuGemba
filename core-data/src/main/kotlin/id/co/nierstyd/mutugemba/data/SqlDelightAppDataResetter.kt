package id.co.nierstyd.mutugemba.data

import id.co.nierstyd.mutugemba.data.db.DatabaseDriver
import id.co.nierstyd.mutugemba.data.db.InMemoryDatabase
import id.co.nierstyd.mutugemba.domain.AppDataResetter
import java.nio.file.Files

class SqlDelightAppDataResetter(
    private val database: InMemoryDatabase,
    private val driver: DatabaseDriver,
) : AppDataResetter {
    override fun resetAll(): Boolean =
        runCatching {
            database.clearAll()
            driver.close()
            Files.deleteIfExists(database.stateFilePath)
            true
        }.getOrDefault(false)
}
