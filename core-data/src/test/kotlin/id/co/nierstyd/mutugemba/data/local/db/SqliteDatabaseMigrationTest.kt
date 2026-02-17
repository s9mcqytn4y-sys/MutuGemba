package id.co.nierstyd.mutugemba.data.local.db

import java.nio.file.Files
import java.sql.DriverManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SqliteDatabaseMigrationTest {
    @Test
    fun migrateIfNeeded_replacesExistingPartTablesWithoutDuplicateTableError() {
        val tempDir = Files.createTempDirectory("mutugemba-sqlite-migrate")
        val dbFile = tempDir.resolve("mapping.db")

        DriverManager.getConnection("jdbc:sqlite:${dbFile.toAbsolutePath()}").use { connection ->
            connection.createStatement().use { statement ->
                statement.execute("CREATE TABLE production_line (production_line_id INTEGER PRIMARY KEY, code TEXT)")
                statement.execute("PRAGMA user_version = 10")
            }
        }

        val database = SqliteDatabase(dbFile)
        val hasPartTable =
            database.read { connection ->
                connection.createStatement().use { statement ->
                    statement.executeQuery(
                        "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='part'",
                    ).use { rs ->
                        rs.next()
                        rs.getInt(1) == 1
                    }
                }
            }
        val userVersion =
            database.read { connection ->
                connection.createStatement().use { statement ->
                    statement.executeQuery("PRAGMA user_version").use { rs ->
                        rs.next()
                        rs.getInt(1)
                    }
                }
            }
        val hasPartReportIndex =
            database.read { connection ->
                connection.prepareStatement(
                    "SELECT COUNT(*) FROM sqlite_master WHERE type='index' AND name='idx_qa_obs_part_report'",
                ).use { statement ->
                    statement.executeQuery().use { rs ->
                        rs.next()
                        rs.getInt(1) == 1
                    }
                }
            }
        val backupCount =
            Files.list(tempDir).use { paths ->
                paths
                    .filter { path -> path.fileName.toString().startsWith("mapping.db.backup-v10-") }
                    .count()
            }

        assertTrue(hasPartTable)
        assertEquals(12, userVersion)
        assertTrue(hasPartReportIndex)
        assertEquals(1L, backupCount)
    }
}
