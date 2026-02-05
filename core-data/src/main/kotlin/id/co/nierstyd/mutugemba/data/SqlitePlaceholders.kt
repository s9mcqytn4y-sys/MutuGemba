package id.co.nierstyd.mutugemba.data

import java.nio.file.Path

data class SqliteConfig(
    val databaseFile: Path,
    val attachmentsDir: Path,
)

object Migrations {
    const val VERSION_1 = 1
}
