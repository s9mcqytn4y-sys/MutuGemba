package id.co.nierstyd.mutugemba.data.local.db

import id.co.nierstyd.mutugemba.domain.model.DefectMaster
import id.co.nierstyd.mutugemba.domain.model.MaterialMaster
import id.co.nierstyd.mutugemba.domain.model.NgOriginType
import id.co.nierstyd.mutugemba.domain.model.PartDefectAssignment
import id.co.nierstyd.mutugemba.domain.model.PartMasterDetail
import id.co.nierstyd.mutugemba.domain.model.PartMasterListItem
import id.co.nierstyd.mutugemba.domain.model.PartMaterialAssignment
import id.co.nierstyd.mutugemba.domain.model.SaveDefectMasterCommand
import id.co.nierstyd.mutugemba.domain.model.SaveMaterialMasterCommand
import id.co.nierstyd.mutugemba.domain.model.SavePartDefectAssignmentCommand
import id.co.nierstyd.mutugemba.domain.model.SavePartMasterCommand
import id.co.nierstyd.mutugemba.domain.model.SupplierMaster
import id.co.nierstyd.mutugemba.domain.repository.PartMasterRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.SQLException
import java.sql.Statement
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class SqlitePartMasterRepository(
    private val database: SqliteDatabase,
) : PartMasterRepository {
    override suspend fun listParts(lineCode: String?): List<PartMasterListItem> =
        withContext(Dispatchers.IO) {
            database.read { connection ->
                connection
                    .prepareStatement(
                        """
                        SELECT
                          p.part_id,
                          p.uniq_no,
                          p.part_number,
                          p.part_name,
                          pl.code AS line_code,
                          COALESCE(pc.exclude_from_checksheet, 0) AS exclude_from_checksheet
                        FROM part p
                        JOIN production_line pl ON pl.production_line_id = p.production_line_id
                        LEFT JOIN part_configuration pc ON pc.part_id = p.part_id
                        WHERE (? IS NULL OR pl.code = ?)
                        ORDER BY p.uniq_no
                        """.trimIndent(),
                    ).use { statement ->
                        statement.setString(1, lineCode)
                        statement.setString(2, lineCode)
                        statement.executeQuery().use { rs ->
                            buildList {
                                while (rs.next()) {
                                    add(
                                        PartMasterListItem(
                                            id = rs.getLong("part_id"),
                                            uniqNo = rs.getString("uniq_no"),
                                            partNumber = rs.getString("part_number"),
                                            partName = rs.getString("part_name"),
                                            lineCode = rs.getString("line_code"),
                                            excludedFromChecksheet = rs.getInt("exclude_from_checksheet") == 1,
                                        ),
                                    )
                                }
                            }
                        }
                    }
            }
        }

    override suspend fun getPartDetail(partId: Long): PartMasterDetail? =
        withContext(Dispatchers.IO) {
            database.read { connection ->
                val base =
                    connection
                        .prepareStatement(
                            """
                            SELECT
                              p.part_id,
                              p.uniq_no,
                              p.part_number,
                              p.part_name,
                              pl.code AS line_code,
                              COALESCE(pc.exclude_from_checksheet, 0) AS exclude_from_checksheet
                            FROM part p
                            JOIN production_line pl ON pl.production_line_id = p.production_line_id
                            LEFT JOIN part_configuration pc ON pc.part_id = p.part_id
                            WHERE p.part_id = ?
                            LIMIT 1
                            """.trimIndent(),
                        ).use { statement ->
                            statement.setLong(1, partId)
                            statement.executeQuery().use { rs ->
                                if (!rs.next()) {
                                    null
                                } else {
                                    PartMasterListItem(
                                        id = rs.getLong("part_id"),
                                        uniqNo = rs.getString("uniq_no"),
                                        partNumber = rs.getString("part_number"),
                                        partName = rs.getString("part_name"),
                                        lineCode = rs.getString("line_code"),
                                        excludedFromChecksheet = rs.getInt("exclude_from_checksheet") == 1,
                                    )
                                }
                            }
                        } ?: return@read null

                val materials =
                    connection
                        .prepareStatement(
                            """
                            SELECT
                              pml.layer_order,
                              m.material_id,
                              m.material_name,
                              s.supplier_name
                            FROM part_material_layer pml
                            JOIN material m ON m.material_id = pml.material_id
                            LEFT JOIN supplier s ON s.supplier_id = m.supplier_id
                            WHERE pml.part_id = ?
                            ORDER BY pml.layer_order
                            """.trimIndent(),
                        ).use { statement ->
                            statement.setLong(1, partId)
                            statement.executeQuery().use { rs ->
                                buildList {
                                    while (rs.next()) {
                                        add(
                                            PartMaterialAssignment(
                                                materialId = rs.getLong("material_id"),
                                                materialName = rs.getString("material_name"),
                                                supplierName = rs.getString("supplier_name"),
                                                layerOrder = rs.getInt("layer_order"),
                                            ),
                                        )
                                    }
                                }
                            }
                        }

                val defects =
                    connection
                        .prepareStatement(
                            """
                            SELECT
                              pdc.defect_catalog_id,
                              dc.defect_name,
                              pdc.ng_origin_type,
                              pdc.material_id,
                              m.material_name
                            FROM part_defect_catalog pdc
                            JOIN defect_catalog dc ON dc.defect_catalog_id = pdc.defect_catalog_id
                            LEFT JOIN material m ON m.material_id = pdc.material_id
                            WHERE pdc.part_id = ?
                            ORDER BY dc.defect_name
                            """.trimIndent(),
                        ).use { statement ->
                            statement.setLong(1, partId)
                            statement.executeQuery().use { rs ->
                                buildList {
                                    while (rs.next()) {
                                        add(
                                            PartDefectAssignment(
                                                defectId = rs.getLong("defect_catalog_id"),
                                                defectName = rs.getString("defect_name"),
                                                originType = rs.getString("ng_origin_type").toNgOriginType(),
                                                materialId = rs.getNullableLong("material_id"),
                                                materialName = rs.getString("material_name"),
                                            ),
                                        )
                                    }
                                }
                            }
                        }

                PartMasterDetail(
                    id = base.id,
                    uniqNo = base.uniqNo,
                    partNumber = base.partNumber,
                    partName = base.partName,
                    lineCode = base.lineCode,
                    excludedFromChecksheet = base.excludedFromChecksheet,
                    materials = materials,
                    defects = defects,
                )
            }
        }

    override suspend fun savePart(command: SavePartMasterCommand): Long =
        withContext(Dispatchers.IO) {
            val normalizedUniq = normalizeCode(command.uniqNo)
            val normalizedPartNumber = normalizeCode(command.partNumber)
            val lineId = if (command.lineCode.equals("sewing", true)) 2L else 1L
            val now = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now())
            database.write { connection ->
                val partId =
                    if (command.id == null) {
                        connection
                            .prepareStatement(
                                """
                                INSERT INTO part(
                                  uniq_no,
                                  uniq_no_norm,
                                  production_line_id,
                                  part_number,
                                  part_number_norm,
                                  part_name,
                                  models_source,
                                  created_at,
                                  updated_at
                                ) VALUES(?, ?, ?, ?, ?, ?, 'manual', ?, ?)
                                """.trimIndent(),
                                Statement.RETURN_GENERATED_KEYS,
                            ).use { statement ->
                                statement.setString(1, command.uniqNo.trim())
                                statement.setString(2, normalizedUniq)
                                statement.setLong(3, lineId)
                                statement.setString(4, command.partNumber.trim())
                                statement.setString(5, normalizedPartNumber)
                                statement.setString(6, command.partName.trim())
                                statement.setString(7, now)
                                statement.setString(8, now)
                                statement.executeUpdate()
                                statement.generatedKeys.use { rs ->
                                    check(rs.next()) { "Gagal membuat part baru." }
                                    rs.getLong(1)
                                }
                            }
                    } else {
                        val existingId = requireNotNull(command.id)
                        connection
                            .prepareStatement(
                                """
                                UPDATE part
                                SET
                                  uniq_no = ?,
                                  uniq_no_norm = ?,
                                  production_line_id = ?,
                                  part_number = ?,
                                  part_number_norm = ?,
                                  part_name = ?,
                                  updated_at = ?
                                WHERE part_id = ?
                                """.trimIndent(),
                            ).use { statement ->
                                statement.setString(1, command.uniqNo.trim())
                                statement.setString(2, normalizedUniq)
                                statement.setLong(3, lineId)
                                statement.setString(4, command.partNumber.trim())
                                statement.setString(5, normalizedPartNumber)
                                statement.setString(6, command.partName.trim())
                                statement.setString(7, now)
                                statement.setLong(8, existingId)
                                statement.executeUpdate()
                            }
                        existingId
                    }

                connection
                    .prepareStatement(
                        """
                        INSERT INTO part_configuration(part_id, exclude_from_checksheet)
                        VALUES(?, ?)
                        ON CONFLICT(part_id) DO UPDATE SET
                          exclude_from_checksheet = excluded.exclude_from_checksheet
                        """.trimIndent(),
                    ).use { statement ->
                        statement.setLong(1, partId)
                        statement.setInt(2, if (command.excludedFromChecksheet) 1 else 0)
                        statement.executeUpdate()
                    }
                partId
            }
        }

    override suspend fun deletePart(partId: Long): Boolean =
        withContext(Dispatchers.IO) {
            database.write { connection ->
                connection.prepareStatement("DELETE FROM part WHERE part_id = ?").use { statement ->
                    statement.setLong(1, partId)
                    statement.executeUpdate() > 0
                }
            }
        }

    override suspend fun replacePartMaterials(
        partId: Long,
        materialIdsInOrder: List<Long>,
    ) {
        withContext(Dispatchers.IO) {
            database.write { connection ->
                connection.prepareStatement("DELETE FROM part_material_layer WHERE part_id = ?").use { statement ->
                    statement.setLong(1, partId)
                    statement.executeUpdate()
                }
                materialIdsInOrder.forEachIndexed { index, materialId ->
                    connection
                        .prepareStatement(
                            """
                            INSERT INTO part_material_layer(part_id, material_id, layer_order, unit)
                            VALUES(?, ?, ?, 'g')
                            """.trimIndent(),
                        ).use { statement ->
                            statement.setLong(1, partId)
                            statement.setLong(2, materialId)
                            statement.setInt(3, index + 1)
                            statement.executeUpdate()
                        }
                }
            }
        }
    }

    override suspend fun replacePartDefects(
        partId: Long,
        assignments: List<SavePartDefectAssignmentCommand>,
    ) {
        withContext(Dispatchers.IO) {
            database.write { connection ->
                connection.prepareStatement("DELETE FROM part_defect_catalog WHERE part_id = ?").use { statement ->
                    statement.setLong(1, partId)
                    statement.executeUpdate()
                }
                assignments.distinctBy { it.defectId }.forEach { assignment ->
                    connection
                        .prepareStatement(
                            """
                            INSERT INTO part_defect_catalog(
                              part_id,
                              defect_catalog_id,
                              ng_origin_type,
                              material_id
                            ) VALUES(?, ?, ?, ?)
                            """.trimIndent(),
                        ).use { statement ->
                            statement.setLong(1, partId)
                            statement.setLong(2, assignment.defectId)
                            statement.setString(3, assignment.originType.toDbValue())
                            val mappedMaterialId = assignment.materialId
                            if (mappedMaterialId == null) {
                                statement.setNull(4, java.sql.Types.INTEGER)
                            } else {
                                statement.setLong(4, mappedMaterialId)
                            }
                            statement.executeUpdate()
                        }
                }
            }
        }
    }

    override suspend fun listMaterials(): List<MaterialMaster> =
        withContext(Dispatchers.IO) {
            database.read { connection ->
                connection
                    .prepareStatement(
                        """
                        SELECT
                          m.material_id,
                          m.material_name,
                          m.supplier_id,
                          s.supplier_name,
                          m.client_supplied
                        FROM material m
                        LEFT JOIN supplier s ON s.supplier_id = m.supplier_id
                        ORDER BY m.material_name
                        """.trimIndent(),
                    ).use { statement ->
                        statement.executeQuery().use { rs ->
                            buildList {
                                while (rs.next()) {
                                    add(
                                        MaterialMaster(
                                            id = rs.getLong("material_id"),
                                            name = rs.getString("material_name"),
                                            supplierId = rs.getNullableLong("supplier_id"),
                                            supplierName = rs.getString("supplier_name"),
                                            clientSupplied = rs.getInt("client_supplied") == 1,
                                        ),
                                    )
                                }
                            }
                        }
                    }
            }
        }

    override suspend fun saveMaterial(command: SaveMaterialMasterCommand): Long =
        withContext(Dispatchers.IO) {
            runCatching {
                database.write { connection ->
                    val normalized = normalizeText(command.name)
                    if (command.id == null) {
                        connection
                            .prepareStatement(
                                """
                                INSERT INTO material(material_name, material_name_norm, supplier_id, client_supplied)
                                VALUES(?, ?, ?, ?)
                                """.trimIndent(),
                                Statement.RETURN_GENERATED_KEYS,
                            ).use { statement ->
                                statement.setString(1, command.name.trim())
                                statement.setString(2, normalized)
                                val supplierId = command.supplierId
                                if (supplierId == null) {
                                    statement.setNull(3, java.sql.Types.INTEGER)
                                } else {
                                    statement.setLong(3, supplierId)
                                }
                                statement.setInt(4, if (command.clientSupplied) 1 else 0)
                                statement.executeUpdate()
                                statement.generatedKeys.use { rs ->
                                    check(rs.next()) { "Gagal menyimpan bahan." }
                                    rs.getLong(1)
                                }
                            }
                    } else {
                        val existingId = requireNotNull(command.id)
                        connection
                            .prepareStatement(
                                """
                                UPDATE material
                                SET material_name = ?, material_name_norm = ?, supplier_id = ?, client_supplied = ?
                                WHERE material_id = ?
                                """.trimIndent(),
                            ).use { statement ->
                                statement.setString(1, command.name.trim())
                                statement.setString(2, normalized)
                                val supplierId = command.supplierId
                                if (supplierId == null) {
                                    statement.setNull(3, java.sql.Types.INTEGER)
                                } else {
                                    statement.setLong(3, supplierId)
                                }
                                statement.setInt(4, if (command.clientSupplied) 1 else 0)
                                statement.setLong(5, existingId)
                                statement.executeUpdate()
                            }
                        existingId
                    }
                }
            }.getOrElse { throwable -> throw throwable.toFriendlyPersistenceException("Bahan") }
        }

    override suspend fun deleteMaterial(materialId: Long): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                database.write { connection ->
                    connection.prepareStatement("DELETE FROM material WHERE material_id = ?").use { statement ->
                        statement.setLong(1, materialId)
                        statement.executeUpdate() > 0
                    }
                }
            }.getOrElse { throwable -> throw throwable.toFriendlyPersistenceException("Bahan") }
        }

    override suspend fun listSuppliers(): List<SupplierMaster> =
        withContext(Dispatchers.IO) {
            database.read { connection ->
                connection
                    .prepareStatement(
                        """
                        SELECT supplier_id, supplier_name, is_active
                        FROM supplier
                        ORDER BY supplier_name
                        """.trimIndent(),
                    ).use { statement ->
                        statement.executeQuery().use { rs ->
                            buildList {
                                while (rs.next()) {
                                    add(
                                        SupplierMaster(
                                            id = rs.getLong("supplier_id"),
                                            name = rs.getString("supplier_name"),
                                            isActive = rs.getInt("is_active") == 1,
                                        ),
                                    )
                                }
                            }
                        }
                    }
            }
        }

    override suspend fun saveSupplier(
        id: Long?,
        name: String,
    ): Long =
        withContext(Dispatchers.IO) {
            runCatching {
                database.write { connection ->
                    val normalized = normalizeText(name)
                    if (id == null) {
                        connection
                            .prepareStatement(
                                """
                                INSERT INTO supplier(supplier_name, supplier_name_norm, is_active)
                                VALUES(?, ?, 1)
                                """.trimIndent(),
                                Statement.RETURN_GENERATED_KEYS,
                            ).use { statement ->
                                statement.setString(1, name.trim())
                                statement.setString(2, normalized)
                                statement.executeUpdate()
                                statement.generatedKeys.use { rs ->
                                    check(rs.next()) { "Gagal menyimpan pemasok." }
                                    rs.getLong(1)
                                }
                            }
                    } else {
                        connection
                            .prepareStatement(
                                """
                                UPDATE supplier
                                SET supplier_name = ?, supplier_name_norm = ?
                                WHERE supplier_id = ?
                                """.trimIndent(),
                            ).use { statement ->
                                statement.setString(1, name.trim())
                                statement.setString(2, normalized)
                                statement.setLong(3, id)
                                statement.executeUpdate()
                            }
                        id
                    }
                }
            }.getOrElse { throwable -> throw throwable.toFriendlyPersistenceException("Pemasok") }
        }

    override suspend fun deleteSupplier(supplierId: Long): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                database.write { connection ->
                    connection.prepareStatement("DELETE FROM supplier WHERE supplier_id = ?").use { statement ->
                        statement.setLong(1, supplierId)
                        statement.executeUpdate() > 0
                    }
                }
            }.getOrElse { throwable -> throw throwable.toFriendlyPersistenceException("Pemasok") }
        }

    override suspend fun listDefects(): List<DefectMaster> =
        withContext(Dispatchers.IO) {
            database.read { connection ->
                connection
                    .prepareStatement(
                        """
                        SELECT defect_catalog_id, defect_name, ng_origin_type, line_code
                        FROM defect_catalog
                        ORDER BY ng_origin_type ASC, defect_name ASC
                        """.trimIndent(),
                    ).use { statement ->
                        statement.executeQuery().use { rs ->
                            buildList {
                                while (rs.next()) {
                                    add(
                                        DefectMaster(
                                            id = rs.getLong("defect_catalog_id"),
                                            name = rs.getString("defect_name"),
                                            originType = rs.getString("ng_origin_type").toNgOriginType(),
                                            lineCode = rs.getString("line_code"),
                                        ),
                                    )
                                }
                            }
                        }
                    }
            }
        }

    override suspend fun saveDefect(command: SaveDefectMasterCommand): Long =
        withContext(Dispatchers.IO) {
            runCatching {
                database.write { connection ->
                    val normalized = normalizeText(command.name)
                    if (command.id == null) {
                        connection
                            .prepareStatement(
                                """
                                INSERT INTO defect_catalog(defect_name, defect_name_norm, ng_origin_type, line_code)
                                VALUES(?, ?, ?, ?)
                                """.trimIndent(),
                                Statement.RETURN_GENERATED_KEYS,
                            ).use { statement ->
                                statement.setString(1, command.name.trim())
                                statement.setString(2, normalized)
                                statement.setString(3, command.originType.toDbValue())
                                statement.setString(4, command.lineCode?.lowercase(Locale.getDefault()))
                                statement.executeUpdate()
                                statement.generatedKeys.use { rs ->
                                    check(rs.next()) { "Gagal menyimpan Jenis NG." }
                                    rs.getLong(1)
                                }
                            }
                    } else {
                        val existingId = requireNotNull(command.id)
                        connection
                            .prepareStatement(
                                """
                                UPDATE defect_catalog
                                SET defect_name = ?, defect_name_norm = ?, ng_origin_type = ?, line_code = ?
                                WHERE defect_catalog_id = ?
                                """.trimIndent(),
                            ).use { statement ->
                                statement.setString(1, command.name.trim())
                                statement.setString(2, normalized)
                                statement.setString(3, command.originType.toDbValue())
                                statement.setString(4, command.lineCode?.lowercase(Locale.getDefault()))
                                statement.setLong(5, existingId)
                                statement.executeUpdate()
                            }
                        existingId
                    }
                }
            }.getOrElse { throwable -> throw throwable.toFriendlyPersistenceException("Jenis NG") }
        }

    override suspend fun deleteDefect(defectId: Long): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                database.write { connection ->
                    connection.prepareStatement("DELETE FROM defect_catalog WHERE defect_catalog_id = ?").use { statement ->
                        statement.setLong(1, defectId)
                        statement.executeUpdate() > 0
                    }
                }
            }.getOrElse { throwable -> throw throwable.toFriendlyPersistenceException("Jenis NG") }
        }
}

private fun normalizeCode(value: String): String =
    value
        .trim()
        .uppercase(Locale.getDefault())
        .replace("O", "0")
        .replace("[^A-Z0-9]".toRegex(), "")

private fun normalizeText(value: String): String =
    value
        .trim()
        .uppercase(Locale.getDefault())
        .replace("\\s+".toRegex(), " ")

private fun java.sql.ResultSet.getNullableLong(column: String): Long? {
    val value = getLong(column)
    return if (wasNull()) null else value
}

private fun NgOriginType.toDbValue(): String = if (this == NgOriginType.MATERIAL) "material" else "process"

private fun String.toNgOriginType(): NgOriginType =
    if (equals("material", true)) NgOriginType.MATERIAL else NgOriginType.PROCESS

private fun Throwable.toFriendlyPersistenceException(entity: String): Throwable {
    val sqlMessage = (this as? SQLException)?.message?.lowercase(Locale.getDefault()).orEmpty()
    return when {
        sqlMessage.contains("unique") ->
            IllegalArgumentException("$entity sudah ada. Gunakan nama yang berbeda.")
        sqlMessage.contains("foreign key") ->
            IllegalArgumentException("$entity masih dipakai relasi data lain, tidak bisa diubah/hapus.")
        else -> this
    }
}
