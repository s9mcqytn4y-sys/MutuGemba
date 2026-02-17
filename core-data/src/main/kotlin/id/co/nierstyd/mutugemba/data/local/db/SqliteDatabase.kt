package id.co.nierstyd.mutugemba.data.local.db

import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager

private const val TARGET_SCHEMA_VERSION = 12
private const val SQLITE_BUSY_TIMEOUT_MS = 5_000
private const val BACKUP_FILE_LABEL = "backup"

class SqliteDatabase(
    private val dbFile: Path,
) {
    private val logger = LoggerFactory.getLogger(SqliteDatabase::class.java)

    init {
        Class.forName("org.sqlite.JDBC")
        Files.createDirectories(dbFile.parent)
        migrateIfNeeded()
    }

    fun <T> read(block: (Connection) -> T): T = withConnection(readOnly = true, block = block)

    fun <T> write(block: (Connection) -> T): T =
        withConnection(readOnly = false) { connection ->
            connection.autoCommit = false
            runCatching { block(connection) }
                .onSuccess { connection.commit() }
                .onFailure { connection.rollback() }
                .getOrThrow()
        }

    private fun <T> withConnection(
        readOnly: Boolean,
        block: (Connection) -> T,
    ): T {
        DriverManager.getConnection("jdbc:sqlite:${dbFile.toAbsolutePath()}").use { connection ->
            connection.createStatement().use { statement ->
                statement.execute("PRAGMA foreign_keys = ON;")
                statement.execute("PRAGMA busy_timeout = $SQLITE_BUSY_TIMEOUT_MS;")
                if (!readOnly) {
                    statement.execute("PRAGMA journal_mode = WAL;")
                    statement.execute("PRAGMA synchronous = NORMAL;")
                }
                if (readOnly) {
                    statement.execute("PRAGMA query_only = ON;")
                } else {
                    statement.execute("PRAGMA query_only = OFF;")
                }
            }
            return block(connection)
        }
    }

    private fun migrateIfNeeded() {
        withConnection(readOnly = false) { connection ->
            val currentVersion =
                connection.createStatement().use { statement ->
                    statement.executeQuery("PRAGMA user_version;").use { rs ->
                        if (rs.next()) rs.getInt(1) else 0
                    }
                }
            val hasNormalizedSchema = hasColumn(connection, table = "part", column = "uniq_no_norm")
            val hasQaObservationPartReportIndex = hasIndex(connection, indexName = "idx_qa_obs_part_report")
            if (!isMigrationRequired(currentVersion, hasNormalizedSchema, hasQaObservationPartReportIndex)) {
                return@withConnection
            }
            val partCountBefore = readPartCount(connection)
            val backupPath = createBackupIfNeeded(currentVersion)
            logger.info(
                "Menjalankan migrasi skema lokal dari versi {} ke {} (backup: {}).",
                currentVersion,
                TARGET_SCHEMA_VERSION,
                backupPath?.toAbsolutePath() ?: "tidak diperlukan",
            )
            applySchemaMigration(connection, loadSchemaSql())
            warnIfPartDataCleared(connection, partCountBefore)
        }
    }

    private fun loadSchemaSql(): String =
        javaClass.classLoader
            .getResourceAsStream("db/schema_v$TARGET_SCHEMA_VERSION.sql")
            ?.bufferedReader(Charsets.UTF_8)
            ?.use { it.readText() }
            ?: error("Missing schema resource: db/schema_v$TARGET_SCHEMA_VERSION.sql")

    private fun isMigrationRequired(
        currentVersion: Int,
        hasNormalizedSchema: Boolean,
        hasQaObservationPartReportIndex: Boolean,
    ): Boolean = currentVersion < TARGET_SCHEMA_VERSION || !hasNormalizedSchema || !hasQaObservationPartReportIndex

    private fun readPartCount(connection: Connection): Int =
        if (hasTable(connection, "part")) countRows(connection, "part") else 0

    private fun applySchemaMigration(
        connection: Connection,
        schemaSql: String,
    ) {
        connection.autoCommit = false
        runCatching {
            schemaSql.statements().forEach { sql ->
                connection.createStatement().use { statement -> statement.execute(sql) }
            }
            connection.createStatement().use { it.execute("PRAGMA user_version = $TARGET_SCHEMA_VERSION;") }
            connection.commit()
        }.onFailure {
            connection.rollback()
            throw it
        }
    }

    private fun warnIfPartDataCleared(
        connection: Connection,
        partCountBefore: Int,
    ) {
        val partCountAfter = readPartCount(connection)
        if (partCountBefore > 0 && partCountAfter == 0) {
            logger.warn(
                "Migrasi skema mengosongkan data part lama. Gunakan file backup sebelum lanjut bootstrap ulang.",
            )
        }
    }

    private fun createBackupIfNeeded(currentVersion: Int): Path? {
        val shouldBackup =
            currentVersion > 0 &&
                Files.exists(dbFile) &&
                runCatching { Files.size(dbFile) }.getOrDefault(0L) > 0L
        if (!shouldBackup) {
            return null
        }
        val backupPath =
            dbFile.resolveSibling(
                "${dbFile.fileName}.$BACKUP_FILE_LABEL-v$currentVersion-${System.currentTimeMillis()}",
            )
        runCatching {
            Files.copy(dbFile, backupPath)
        }.getOrElse { throwable ->
            throw IllegalStateException(
                "Backup migrasi gagal dibuat. Proses dihentikan agar data lokal tetap aman.",
                throwable,
            )
        }
        return backupPath
    }
}

private fun hasColumn(
    connection: Connection,
    table: String,
    column: String,
): Boolean =
    connection.createStatement().use { statement ->
        statement.executeQuery("PRAGMA table_info($table)").use { rs ->
            buildList {
                while (rs.next()) {
                    add(rs.getString("name"))
                }
            }.any { it == column }
        }
    }

private fun hasIndex(
    connection: Connection,
    indexName: String,
): Boolean =
    connection
        .prepareStatement(
            "SELECT 1 FROM sqlite_master WHERE type = 'index' AND name = ? LIMIT 1",
        ).use { statement ->
            statement.setString(1, indexName)
            statement.executeQuery().use { rs -> rs.next() }
        }

private fun hasTable(
    connection: Connection,
    tableName: String,
): Boolean =
    connection
        .prepareStatement(
            "SELECT 1 FROM sqlite_master WHERE type='table' AND name=? LIMIT 1",
        ).use { statement ->
            statement.setString(1, tableName)
            statement.executeQuery().use { rs -> rs.next() }
        }

private fun countRows(
    connection: Connection,
    tableName: String,
): Int =
    connection.createStatement().use { statement ->
        statement.executeQuery("SELECT COUNT(*) FROM $tableName").use { rs ->
            if (rs.next()) rs.getInt(1) else 0
        }
    }

private fun String.statements(): List<String> {
    val statements = mutableListOf<String>()
    val builder = StringBuilder()
    var inSingleQuote = false
    var inDoubleQuote = false

    for (ch in this) {
        when (ch) {
            '\'' -> {
                if (!inDoubleQuote) inSingleQuote = !inSingleQuote
                builder.append(ch)
            }

            '"' -> {
                if (!inSingleQuote) inDoubleQuote = !inDoubleQuote
                builder.append(ch)
            }

            ';' -> {
                if (inSingleQuote || inDoubleQuote) {
                    builder.append(ch)
                } else {
                    val sql = builder.toString().trim()
                    if (sql.isNotEmpty()) statements += sql
                    builder.clear()
                }
            }

            else -> builder.append(ch)
        }
    }

    val tail = builder.toString().trim()
    if (tail.isNotEmpty()) statements += tail
    return statements
}
