package id.co.nierstyd.mutugemba.data.local.db

import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val TARGET_SCHEMA_VERSION = 12
private const val DEFAULT_SUPPLIER_NAME = "PT Mitra Prima Sentosa"
private const val DEFAULT_SUPPLIER_NAME_NORM = "PT MITRA PRIMA SENTOSA"

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
        runCatching { migrateInternal() }.onFailure { throwable ->
            val message = throwable.message?.lowercase(Locale.getDefault()).orEmpty()
            if (message.contains("not a database")) {
                logger.warn("Database file invalid. Backup corrupted file and rebuild schema.")
                backupCorruptedDatabase()
                migrateInternal()
            } else {
                throw throwable
            }
        }
    }

    private fun migrateInternal() {
        withConnection(readOnly = false) { connection ->
            val currentVersion =
                connection.createStatement().use { statement ->
                    statement.executeQuery("PRAGMA user_version;").use { rs ->
                        if (rs.next()) rs.getInt(1) else 0
                    }
                }
            val hasNormalizedSchema = hasColumn(connection, table = "part", column = "uniq_no_norm")
            val hasAdvancedMasterSchema =
                hasTable(connection, "part_configuration") && hasTable(connection, "defect_catalog")
            if (currentVersion >= TARGET_SCHEMA_VERSION && hasNormalizedSchema && hasAdvancedMasterSchema) {
                ensureReferenceData(connection)
                return@withConnection
            }

            if (hasNormalizedSchema && (currentVersion >= 11 || !hasAdvancedMasterSchema)) {
                logger.info(
                    "Repairing supplemental schema objects for version {} (target {}).",
                    currentVersion,
                    TARGET_SCHEMA_VERSION,
                )
                connection.autoCommit = false
                runCatching {
                    ensureSupplementalSchema(connection)
                    ensureReferenceData(connection)
                    connection.createStatement().use { it.execute("PRAGMA user_version = $TARGET_SCHEMA_VERSION;") }
                    connection.commit()
                }.onFailure {
                    connection.rollback()
                    throw it
                }
                return@withConnection
            }

            logger.info(
                "Applying hard-replace schema migration from version {} to {}.",
                currentVersion,
                TARGET_SCHEMA_VERSION,
            )
            val schemaSql = loadSchemaSql()
            connection.autoCommit = false
            runCatching {
                schemaSql.statements().forEach { sql ->
                    connection.createStatement().use { statement -> statement.execute(sql) }
                }
                ensureReferenceData(connection)
                connection.createStatement().use { it.execute("PRAGMA user_version = $TARGET_SCHEMA_VERSION;") }
                connection.commit()
            }.onFailure {
                connection.rollback()
                throw it
            }
        }
    }

    private fun backupCorruptedDatabase() {
        if (!Files.exists(dbFile)) return
        val stamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now())
        val backupPath = dbFile.resolveSibling("${dbFile.fileName}.corrupted-$stamp.bak")
        Files.move(dbFile, backupPath)
    }

    private fun loadSchemaSql(): String =
        javaClass.classLoader
            .getResourceAsStream("db/schema_v12.sql")
            ?.bufferedReader(Charsets.UTF_8)
            ?.use { it.readText() }
            ?: error("Missing schema resource: db/schema_v12.sql")

    private fun hasColumn(
        connection: Connection,
        table: String,
        column: String,
    ): Boolean =
        connection.createStatement().use { statement ->
            statement.executeQuery("PRAGMA table_info($table)").use { rs ->
                while (rs.next()) {
                    if (rs.getString("name") == column) {
                        return@use true
                    }
                }
                false
            }
        }

    private fun hasTable(
        connection: Connection,
        table: String,
    ): Boolean =
        connection.createStatement().use { statement ->
            statement
                .executeQuery(
                    "SELECT COUNT(*) AS total FROM sqlite_master WHERE type = 'table' AND name = '$table'",
                ).use { rs ->
                    rs.next()
                    rs.getInt("total") > 0
                }
        }

    @Suppress("LongMethod")
    private fun ensureSupplementalSchema(connection: Connection) {
        val statements =
            buildList {
                add(
                    """
                    CREATE TABLE IF NOT EXISTS supplier (
                      supplier_id INTEGER PRIMARY KEY AUTOINCREMENT,
                      supplier_name TEXT NOT NULL,
                      supplier_name_norm TEXT NOT NULL UNIQUE,
                      is_active INTEGER NOT NULL DEFAULT 1 CHECK (is_active IN (0, 1))
                    )
                    """.trimIndent(),
                )
                add(
                    """
                    INSERT OR IGNORE INTO supplier(supplier_name, supplier_name_norm, is_active)
                    VALUES ('$DEFAULT_SUPPLIER_NAME', '$DEFAULT_SUPPLIER_NAME_NORM', 1)
                    """.trimIndent(),
                )
                if (!hasColumn(connection, "material", "supplier_id")) {
                    add("ALTER TABLE material ADD COLUMN supplier_id INTEGER REFERENCES supplier(supplier_id)")
                }
                if (!hasColumn(connection, "material", "client_supplied")) {
                    add(
                        """
                        ALTER TABLE material
                        ADD COLUMN client_supplied INTEGER NOT NULL DEFAULT 0
                        CHECK (client_supplied IN (0, 1))
                        """.trimIndent(),
                    )
                }
                add(
                    """
                    CREATE TABLE IF NOT EXISTS defect_catalog (
                      defect_catalog_id INTEGER PRIMARY KEY AUTOINCREMENT,
                      defect_name TEXT NOT NULL,
                      defect_name_norm TEXT NOT NULL UNIQUE,
                      ng_origin_type TEXT NOT NULL CHECK (ng_origin_type IN ('material', 'process')),
                      line_code TEXT CHECK (line_code IS NULL OR line_code IN ('press', 'sewing', 'mixed'))
                    )
                    """.trimIndent(),
                )
                add(
                    """
                    CREATE TABLE IF NOT EXISTS material_defect_catalog (
                      material_id INTEGER NOT NULL,
                      defect_catalog_id INTEGER NOT NULL,
                      PRIMARY KEY (material_id, defect_catalog_id),
                      FOREIGN KEY (material_id) REFERENCES material(material_id) ON DELETE CASCADE,
                      FOREIGN KEY (defect_catalog_id) REFERENCES defect_catalog(defect_catalog_id) ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                add(
                    """
                    CREATE TABLE IF NOT EXISTS part_defect_catalog (
                      part_id INTEGER NOT NULL,
                      defect_catalog_id INTEGER NOT NULL,
                      ng_origin_type TEXT NOT NULL CHECK (ng_origin_type IN ('material', 'process')),
                      material_id INTEGER,
                      PRIMARY KEY (part_id, defect_catalog_id),
                      FOREIGN KEY (part_id) REFERENCES part(part_id) ON DELETE CASCADE,
                      FOREIGN KEY (defect_catalog_id) REFERENCES defect_catalog(defect_catalog_id) ON DELETE CASCADE,
                      FOREIGN KEY (material_id) REFERENCES material(material_id) ON DELETE SET NULL
                    )
                    """.trimIndent(),
                )
                add(
                    """
                    CREATE TABLE IF NOT EXISTS part_configuration (
                      part_id INTEGER PRIMARY KEY,
                      exclude_from_checksheet INTEGER NOT NULL DEFAULT 0 CHECK (exclude_from_checksheet IN (0, 1)),
                      is_recycled_part INTEGER NOT NULL DEFAULT 0 CHECK (is_recycled_part IN (0, 1)),
                      recycle_note TEXT,
                      FOREIGN KEY (part_id) REFERENCES part(part_id) ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                add(
                    """
                    CREATE TABLE IF NOT EXISTS part_recycle_source (
                      recycle_part_id INTEGER NOT NULL,
                      source_part_id INTEGER,
                      source_material_id INTEGER,
                      note TEXT,
                      PRIMARY KEY (recycle_part_id, source_part_id, source_material_id),
                      FOREIGN KEY (recycle_part_id) REFERENCES part(part_id) ON DELETE CASCADE,
                      FOREIGN KEY (source_part_id) REFERENCES part(part_id) ON DELETE SET NULL,
                      FOREIGN KEY (source_material_id) REFERENCES material(material_id) ON DELETE SET NULL
                    )
                    """.trimIndent(),
                )
                add("CREATE INDEX IF NOT EXISTS idx_material_supplier ON material(supplier_id)")
                add("CREATE INDEX IF NOT EXISTS idx_material_client_supplied ON material(client_supplied)")
                add("CREATE INDEX IF NOT EXISTS idx_defect_catalog_origin ON defect_catalog(ng_origin_type)")
                add("CREATE INDEX IF NOT EXISTS idx_defect_catalog_line ON defect_catalog(line_code)")
                add(
                    """
                    CREATE INDEX IF NOT EXISTS idx_material_defect_catalog_defect
                    ON material_defect_catalog(defect_catalog_id)
                    """.trimIndent(),
                )
                add(
                    """
                    CREATE INDEX IF NOT EXISTS idx_part_defect_catalog_origin
                    ON part_defect_catalog(part_id, ng_origin_type)
                    """.trimIndent(),
                )
                add("CREATE INDEX IF NOT EXISTS idx_part_defect_catalog_material ON part_defect_catalog(material_id)")
                add("CREATE INDEX IF NOT EXISTS idx_part_recycle_source_part ON part_recycle_source(recycle_part_id)")
            }

        statements.forEach { sql ->
            connection.createStatement().use { statement -> statement.execute(sql) }
        }
    }

    private fun ensureReferenceData(connection: Connection) {
        if (!hasTable(connection, "supplier")) return
        connection.createStatement().use { statement ->
            statement.execute(
                """
                UPDATE supplier
                SET supplier_name = '$DEFAULT_SUPPLIER_NAME',
                    supplier_name_norm = '$DEFAULT_SUPPLIER_NAME_NORM'
                WHERE supplier_name_norm = 'PT DUMMY'
                """.trimIndent(),
            )
            statement.execute(
                """
                INSERT OR IGNORE INTO supplier(supplier_name, supplier_name_norm, is_active)
                VALUES ('$DEFAULT_SUPPLIER_NAME', '$DEFAULT_SUPPLIER_NAME_NORM', 1)
                """.trimIndent(),
            )
        }
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
