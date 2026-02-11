package id.co.nierstyd.mutugemba.data.local.db

import id.co.nierstyd.mutugemba.domain.model.PartDetail
import id.co.nierstyd.mutugemba.domain.model.PartFilter
import id.co.nierstyd.mutugemba.domain.model.PartImage
import id.co.nierstyd.mutugemba.domain.model.PartListItem
import id.co.nierstyd.mutugemba.domain.model.PartMaterialDefectRisk
import id.co.nierstyd.mutugemba.domain.model.PartMaterialLayer
import id.co.nierstyd.mutugemba.domain.model.PartRequirement
import id.co.nierstyd.mutugemba.domain.repository.PartRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class SqlitePartRepository(
    private val database: SqliteDatabase,
) : PartRepository {
    override fun observeParts(filter: PartFilter): Flow<List<PartListItem>> =
        flow {
            val normalizedSearch = filter.search?.normalizePartSearch()
            val result =
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
                              GROUP_CONCAT(DISTINCT m.code) AS model_codes,
                              CASE
                                WHEN ? IS NULL OR ? IS NULL THEN 0
                                ELSE COALESCE(
                                  (
                                    SELECT SUM(qo.qty)
                                    FROM qa_part_defect_observation qo
                                    JOIN qa_report qr ON qr.qa_report_id = qo.qa_report_id
                                    WHERE qo.part_id = p.part_id
                                      AND qr.period_year = ?
                                      AND qr.period_month = ?
                                  ),
                                  0
                                )
                              END AS total_defect_mtd
                            FROM part p
                            JOIN production_line pl ON pl.production_line_id = p.production_line_id
                            LEFT JOIN part_model pm ON pm.part_id = p.part_id
                            LEFT JOIN model m ON m.model_id = pm.model_id
                            WHERE (? IS NULL OR pl.code = ?)
                              AND (
                                ? IS NULL OR EXISTS (
                                  SELECT 1
                                  FROM part_model pmf
                                  JOIN model mf ON mf.model_id = pmf.model_id
                                  WHERE pmf.part_id = p.part_id
                                    AND mf.code = ?
                                )
                              )
                              AND (
                                ? IS NULL OR ? = '' OR
                                p.uniq_no LIKE '%' || ? || '%' OR
                                p.part_number LIKE '%' || ? || '%' OR
                                p.part_name LIKE '%' || ? || '%' OR
                                p.part_number_norm LIKE '%' || ? || '%'
                              )
                            GROUP BY p.part_id
                            ORDER BY p.uniq_no
                            LIMIT ? OFFSET ?
                            """.trimIndent(),
                        ).use { statement ->
                            statement.setNullableInt(1, filter.year)
                            statement.setNullableInt(2, filter.month)
                            statement.setNullableInt(3, filter.year)
                            statement.setNullableInt(4, filter.month)
                            statement.setString(5, filter.lineCode)
                            statement.setString(6, filter.lineCode)
                            statement.setString(7, filter.modelCode)
                            statement.setString(8, filter.modelCode)
                            statement.setString(9, filter.search)
                            statement.setString(10, filter.search)
                            statement.setString(11, filter.search)
                            statement.setString(12, filter.search)
                            statement.setString(13, filter.search)
                            statement.setString(14, normalizedSearch)
                            statement.setInt(15, filter.limit)
                            statement.setInt(16, filter.offset)

                            statement.executeQuery().use { rs ->
                                buildList {
                                    while (rs.next()) {
                                        val modelCodes =
                                            rs
                                                .getString("model_codes")
                                                ?.split(',')
                                                ?.map { it.trim() }
                                                ?.filter { it.isNotEmpty() }
                                                ?: emptyList()
                                        add(
                                            PartListItem(
                                                partId = rs.getLong("part_id"),
                                                uniqNo = rs.getString("uniq_no"),
                                                partNumber = rs.getString("part_number"),
                                                partName = rs.getString("part_name"),
                                                lineCode = rs.getString("line_code"),
                                                modelCodes = modelCodes,
                                                totalDefectMonthToDate = rs.getInt("total_defect_mtd"),
                                            ),
                                        )
                                    }
                                }
                            }
                        }
                }
            emit(result)
        }.flowOn(Dispatchers.IO)

    override suspend fun getPartDetail(
        uniqNo: String,
        year: Int,
        month: Int,
    ): PartDetail? =
        kotlinx.coroutines.withContext(Dispatchers.IO) {
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
                              pl.code AS line_code
                            FROM part p
                            JOIN production_line pl ON pl.production_line_id = p.production_line_id
                            WHERE p.uniq_no = ?
                            LIMIT 1
                            """.trimIndent(),
                        ).use { statement ->
                            statement.setString(1, uniqNo)
                            statement.executeQuery().use { rs ->
                                if (!rs.next()) {
                                    null
                                } else {
                                    PartBase(
                                        partId = rs.getLong("part_id"),
                                        uniqNo = rs.getString("uniq_no"),
                                        partNumber = rs.getString("part_number"),
                                        partName = rs.getString("part_name"),
                                        lineCode = rs.getString("line_code"),
                                    )
                                }
                            }
                        } ?: return@read null

                val models =
                    connection
                        .prepareStatement(
                            """
                            SELECT m.code
                            FROM part_model pm
                            JOIN model m ON m.model_id = pm.model_id
                            WHERE pm.part_id = ?
                            ORDER BY m.code
                            """.trimIndent(),
                        ).use { statement ->
                            statement.setLong(1, base.partId)
                            statement.executeQuery().use { rs ->
                                buildList {
                                    while (rs.next()) {
                                        add(rs.getString("code"))
                                    }
                                }
                            }
                        }

                val image =
                    connection
                        .prepareStatement(
                            """
                            SELECT
                              status,
                              storage_relpath,
                              sha256,
                              mime,
                              size_bytes,
                              width_px,
                              height_px
                            FROM image_asset
                            WHERE part_id = ?
                              AND active = 1
                            LIMIT 1
                            """.trimIndent(),
                        ).use { statement ->
                            statement.setLong(1, base.partId)
                            statement.executeQuery().use { rs ->
                                if (!rs.next()) {
                                    null
                                } else {
                                    PartImage(
                                        status = rs.getString("status"),
                                        storageRelPath = rs.getString("storage_relpath"),
                                        sha256 = rs.getString("sha256"),
                                        mime = rs.getString("mime"),
                                        sizeBytes = rs.getLong("size_bytes"),
                                        widthPx = rs.getIntOrNull("width_px"),
                                        heightPx = rs.getIntOrNull("height_px"),
                                    )
                                }
                            }
                        }

                val materials =
                    connection
                        .prepareStatement(
                            """
                            SELECT
                              pml.layer_order,
                              mat.material_name,
                              pml.weight_g,
                              pml.basis_weight_gsm,
                              pml.unit
                            FROM part_material_layer pml
                            JOIN material mat ON mat.material_id = pml.material_id
                            WHERE pml.part_id = ?
                            ORDER BY pml.layer_order
                            """.trimIndent(),
                        ).use { statement ->
                            statement.setLong(1, base.partId)
                            statement.executeQuery().use { rs ->
                                buildList {
                                    while (rs.next()) {
                                        add(
                                            PartMaterialLayer(
                                                layerOrder = rs.getInt("layer_order"),
                                                materialName = rs.getString("material_name"),
                                                weightG = rs.getDoubleOrNull("weight_g"),
                                                basisWeightGsm = rs.getDoubleOrNull("basis_weight_gsm"),
                                                unit = rs.getString("unit"),
                                            ),
                                        )
                                    }
                                }
                            }
                        }

                val requirements =
                    connection
                        .prepareStatement(
                            """
                            SELECT m.code AS model_code, pr.qty_kbn
                            FROM part_requirement pr
                            JOIN model m ON m.model_id = pr.model_id
                            WHERE pr.part_id = ?
                            ORDER BY m.code
                            """.trimIndent(),
                        ).use { statement ->
                            statement.setLong(1, base.partId)
                            statement.executeQuery().use { rs ->
                                buildList {
                                    while (rs.next()) {
                                        add(
                                            PartRequirement(
                                                modelCode = rs.getString("model_code"),
                                                qtyKbn = rs.getInt("qty_kbn"),
                                            ),
                                        )
                                    }
                                }
                            }
                        }

                val materialDefectRisks =
                    connection
                        .prepareStatement(
                            """
                            SELECT
                              ide.defect_name,
                              mir.source_line,
                              mir.risk_score,
                              mir.affected_parts
                            FROM part_material_layer pml
                            JOIN material_item_defect_risk mir ON mir.material_id = pml.material_id
                            JOIN item_defect ide ON ide.item_defect_id = mir.item_defect_id
                            WHERE pml.part_id = ?
                            ORDER BY mir.risk_score DESC, ide.defect_name ASC
                            LIMIT 12
                            """.trimIndent(),
                        ).use { statement ->
                            statement.setLong(1, base.partId)
                            statement.executeQuery().use { rs ->
                                buildList {
                                    while (rs.next()) {
                                        add(
                                            PartMaterialDefectRisk(
                                                defectName = rs.getString("defect_name"),
                                                sourceLine = rs.getString("source_line"),
                                                riskScore = rs.getDouble("risk_score"),
                                                affectedParts = rs.getInt("affected_parts"),
                                            ),
                                        )
                                    }
                                }
                            }
                        }

                val totalDefect =
                    connection
                        .prepareStatement(
                            """
                            SELECT COALESCE(SUM(qo.qty), 0) AS total_defect
                            FROM qa_part_defect_observation qo
                            JOIN qa_report qr ON qr.qa_report_id = qo.qa_report_id
                            WHERE qo.part_id = ?
                              AND qr.period_year = ?
                              AND qr.period_month = ?
                            """.trimIndent(),
                        ).use { statement ->
                            statement.setLong(1, base.partId)
                            statement.setInt(2, year)
                            statement.setInt(3, month)
                            statement.executeQuery().use { rs ->
                                if (rs.next()) rs.getInt("total_defect") else 0
                            }
                        }

                PartDetail(
                    partId = base.partId,
                    uniqNo = base.uniqNo,
                    partNumber = base.partNumber,
                    partName = base.partName,
                    lineCode = base.lineCode,
                    models = models,
                    image = image,
                    materials = materials,
                    materialDefectRisks = materialDefectRisks,
                    requirements = requirements,
                    totalDefectMonthToDate = totalDefect,
                )
            }
        }
}

private data class PartBase(
    val partId: Long,
    val uniqNo: String,
    val partNumber: String,
    val partName: String,
    val lineCode: String,
)

private fun java.sql.ResultSet.getIntOrNull(column: String): Int? {
    val value = getInt(column)
    return if (wasNull()) null else value
}

private fun java.sql.ResultSet.getDoubleOrNull(column: String): Double? {
    val value = getDouble(column)
    return if (wasNull()) null else value
}

private fun java.sql.PreparedStatement.setNullableInt(
    index: Int,
    value: Int?,
) {
    if (value == null) {
        setNull(index, java.sql.Types.INTEGER)
    } else {
        setInt(index, value)
    }
}

private fun String.normalizePartSearch(): String =
    trim()
        .uppercase()
        .replace("O", "0")
        .replace("[^A-Z0-9]".toRegex(), "")
