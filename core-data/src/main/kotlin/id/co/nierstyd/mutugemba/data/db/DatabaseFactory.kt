package id.co.nierstyd.mutugemba.data.db

import java.nio.file.Path

data class DatabaseHandle(
    val database: InMemoryDatabase,
    val driver: DatabaseDriver,
)

class DatabaseDriver {
    fun close() {
        // No-op for in-memory fallback.
    }
}

object DatabaseFactory {
    private fun runtimeStateFile(databaseFile: Path): Path =
        databaseFile
            .resolveSibling("${databaseFile.fileName}.runtime.json")
            .toAbsolutePath()
            .normalize()

    fun createDatabaseHandle(databaseFile: Path): DatabaseHandle =
        DatabaseHandle(
            database = InMemoryDatabase(runtimeStateFile(databaseFile)),
            driver = DatabaseDriver(),
        )
}
