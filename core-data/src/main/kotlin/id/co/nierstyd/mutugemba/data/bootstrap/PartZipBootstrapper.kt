package id.co.nierstyd.mutugemba.data.bootstrap

import id.co.nierstyd.mutugemba.data.local.db.SqliteDatabase
import id.co.nierstyd.mutugemba.domain.model.AssetKey
import id.co.nierstyd.mutugemba.domain.repository.AssetStore
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.sql.Connection
import java.sql.Types
import java.time.Instant
import javax.imageio.ImageIO

data class BootstrapSummary(
    val importedParts: Int,
    val importedImages: Int,
    val importedReports: Int,
)

class PartZipBootstrapper(
    private val database: SqliteDatabase,
    private val assetStore: AssetStore,
    private val json: Json = Json { ignoreUnknownKeys = true },
    private val logger: Logger = LoggerFactory.getLogger(PartZipBootstrapper::class.java),
) {
    fun bootstrapFromExtractedDirIfEmpty(extractedRoot: Path): BootstrapSummary? =
        bootstrapFromExtractedDir(extractedRoot, force = false)

    fun bootstrapFromExtractedDir(
        extractedRoot: Path,
        force: Boolean,
    ): BootstrapSummary? {
        val mappingPath = extractedRoot.resolve("mappings").resolve("mapping.json")
        if (!Files.exists(mappingPath)) {
            logger.info("Extracted mapping not found at {}", mappingPath)
            return null
        }
        val screeningPath = extractedRoot.resolve("reports").resolve("defect_screening.json")
        val screeningBytes = if (Files.exists(screeningPath)) Files.readAllBytes(screeningPath) else null

        val mappingBytes = Files.readAllBytes(mappingPath)
        return bootstrapIfPartEmpty(
            sourceLabel = "folder:$extractedRoot",
            mappingBytes = mappingBytes,
            screeningBytes = screeningBytes,
            force = force,
            readImageBytes = { relPath ->
                val candidate = extractedRoot.resolve(relPath.replace('/', java.io.File.separatorChar))
                if (Files.exists(candidate)) {
                    Files.readAllBytes(candidate)
                } else {
                    null
                }
            },
        )
    }

    private fun bootstrapIfPartEmpty(
        sourceLabel: String,
        mappingBytes: ByteArray,
        screeningBytes: ByteArray? = null,
        force: Boolean,
        readImageBytes: (String) -> ByteArray?,
    ): BootstrapSummary? {
        if (!force && !isPartTableEmpty()) {
            return null
        }

        logger.info("Bootstrapping part data from {}", sourceLabel)
        val mapping = json.decodeFromString(MappingRootDto.serializer(), mappingBytes.decodeToString())
        val summary =
            database.write { connection ->
                resetPartDomain(connection)
                val lineIdByCode = upsertLines(connection)
                val modelIdByCode = mutableMapOf<String, Long>()
                val tagIdByTag = mutableMapOf<String, Long>()
                val materialIdByNorm = mutableMapOf<String, Long>()
                val partIdByUniq = mutableMapOf<String, Long>()
                val partIdByPartNumberNorm = mutableMapOf<String, Long>()

                mapping.indexes.models
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .forEach { code -> modelIdByCode[code] = upsertModel(connection, code) }
                mapping.indexes.material_tags
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .forEach { tag -> tagIdByTag[tag] = upsertTag(connection, tag) }

                var imageCount = 0
                mapping.parts.forEach { part ->
                    val lineId =
                        lineIdByCode[part.production_line.trim().lowercase()] ?: lineIdByCode.getValue("press")
                    val partId = upsertPart(connection, part, lineId)
                    partIdByUniq[part.uniq_no] = partId
                    partIdByPartNumberNorm[normalizePartNumber(part.part_number)] = partId

                    syncPartModels(connection, partId, part, modelIdByCode)
                    syncPartRequirements(connection, partId, part, modelIdByCode)
                    syncPartMaterials(connection, partId, part, materialIdByNorm, tagIdByTag)

                    val imageStored = storePartImage(connection, partId, part, readImageBytes)
                    if (imageStored) {
                        imageCount += 1
                    }
                }

                val reportIdByCode = mutableMapOf<String, Long>()
                mapping.qa.reports.forEach { report ->
                    val lineId = lineIdByCode[report.line.trim().lowercase()] ?: lineIdByCode.getValue("press")
                    reportIdByCode[report.report_id] = upsertQaReport(connection, report, lineId)
                }
                val defectIdByCode = mutableMapOf<String, Long>()
                mapping.qa.defect_types.forEach { defect ->
                    defectIdByCode[defect.defect_type_id] = upsertQaDefect(connection, defect)
                }
                mapping.qa.report_legends.forEach { legend ->
                    val reportId = reportIdByCode[legend.report_id] ?: return@forEach
                    val defectId = defectIdByCode[legend.defect_type_id] ?: return@forEach
                    connection
                        .prepareStatement(
                            """
                            INSERT INTO qa_report_defect_legend(qa_report_id, legend_code, qa_defect_type_id, defect_name_in_report)
                            VALUES(?, ?, ?, ?)
                            ON CONFLICT(qa_report_id, legend_code) DO UPDATE SET
                              qa_defect_type_id = excluded.qa_defect_type_id,
                              defect_name_in_report = excluded.defect_name_in_report
                            """.trimIndent(),
                        ).use { statement ->
                            statement.setLong(1, reportId)
                            statement.setString(2, legend.code)
                            statement.setLong(3, defectId)
                            statement.setString(4, legend.defect_name)
                            statement.executeUpdate()
                        }
                }
                mapping.qa.part_summaries.forEach { summaryRow ->
                    val reportId = reportIdByCode[summaryRow.report_id] ?: return@forEach
                    val partId = partIdByUniq[summaryRow.uniq_no] ?: return@forEach
                    connection
                        .prepareStatement(
                            """
                            INSERT INTO qa_part_report_summary(summary_id, qa_report_id, part_id, uniq_no_in_report, total_check, total_ok, total_defect, source)
                            VALUES(?, ?, ?, ?, ?, ?, ?, ?)
                            ON CONFLICT(summary_id) DO UPDATE SET
                              qa_report_id = excluded.qa_report_id,
                              part_id = excluded.part_id,
                              uniq_no_in_report = excluded.uniq_no_in_report,
                              total_check = excluded.total_check,
                              total_ok = excluded.total_ok,
                              total_defect = excluded.total_defect,
                              source = excluded.source
                            """.trimIndent(),
                        ).use { statement ->
                            statement.setString(1, summaryRow.summary_id)
                            statement.setLong(2, reportId)
                            statement.setLong(3, partId)
                            statement.setString(4, summaryRow.uniq_no)
                            statement.setNullableInt(5, summaryRow.total_check)
                            statement.setNullableInt(6, summaryRow.total_ok)
                            statement.setNullableInt(7, summaryRow.total_defect)
                            statement.setString(8, summaryRow.source)
                            statement.executeUpdate()
                        }
                }
                mapping.qa.observations.forEach { observation ->
                    val reportId = reportIdByCode[observation.report_id] ?: return@forEach
                    val partId = partIdByUniq[observation.uniq_no] ?: return@forEach
                    val defectId = defectIdByCode[observation.defect_type_id] ?: return@forEach
                    connection
                        .prepareStatement(
                            """
                            INSERT INTO qa_part_defect_observation(
                              observation_id, qa_report_id, part_id, uniq_no_in_report, part_number_in_report,
                              qa_defect_type_id, defect_name_in_report, qty, source
                            ) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)
                            ON CONFLICT(observation_id) DO UPDATE SET
                              qa_report_id = excluded.qa_report_id,
                              part_id = excluded.part_id,
                              uniq_no_in_report = excluded.uniq_no_in_report,
                              part_number_in_report = excluded.part_number_in_report,
                              qa_defect_type_id = excluded.qa_defect_type_id,
                              defect_name_in_report = excluded.defect_name_in_report,
                              qty = excluded.qty,
                              source = excluded.source
                            """.trimIndent(),
                        ).use { statement ->
                            statement.setString(1, observation.observation_id)
                            statement.setLong(2, reportId)
                            statement.setLong(3, partId)
                            statement.setString(4, observation.uniq_no)
                            statement.setString(5, observation.part_number_in_report)
                            statement.setLong(6, defectId)
                            statement.setString(7, observation.defect_name)
                            statement.setInt(8, observation.qty)
                            statement.setString(9, observation.source)
                            statement.executeUpdate()
                        }
                }

                if (screeningBytes != null) {
                    importDefectScreening(
                        connection = connection,
                        screening =
                            json.decodeFromString(
                                DefectScreeningDto.serializer(),
                                screeningBytes.decodeToString(),
                            ),
                        partIdByPartNumberNorm = partIdByPartNumberNorm,
                        materialIdByNorm = materialIdByNorm,
                    )
                }

                BootstrapSummary(
                    importedParts = mapping.parts.size,
                    importedImages = imageCount,
                    importedReports = mapping.qa.reports.size,
                )
            }

        logger.info(
            "Part bootstrap completed from {}: parts={}, images={}, reports={}",
            sourceLabel,
            summary.importedParts,
            summary.importedImages,
            summary.importedReports,
        )
        return summary
    }

    private fun isPartTableEmpty(): Boolean =
        database.read { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery("SELECT COUNT(*) FROM part").use { rs ->
                    rs.next()
                    rs.getInt(1) <= 0
                }
            }
        }

    private fun resetPartDomain(connection: Connection) {
        val sql =
            listOf(
                "DELETE FROM qa_part_defect_observation",
                "DELETE FROM qa_part_report_summary",
                "DELETE FROM qa_report_defect_legend",
                "DELETE FROM qa_defect_type",
                "DELETE FROM qa_report",
                "DELETE FROM image_asset",
                "DELETE FROM material_tag_model",
                "DELETE FROM part_material_layer_tag",
                "DELETE FROM part_material_tag",
                "DELETE FROM part_material_layer",
                "DELETE FROM material",
                "DELETE FROM material_tag",
                "DELETE FROM part_requirement",
                "DELETE FROM part_model",
                "DELETE FROM model",
                "DELETE FROM part",
            )
        sql.forEach { row ->
            connection.createStatement().use { it.executeUpdate(row) }
        }
    }

    private fun upsertLines(connection: Connection): Map<String, Long> {
        connection
            .prepareStatement(
                "INSERT OR IGNORE INTO production_line(production_line_id, code, display_name) VALUES (1, 'press', 'Press')",
            ).use { it.executeUpdate() }
        connection
            .prepareStatement(
                "INSERT OR IGNORE INTO production_line(production_line_id, code, display_name) VALUES (2, 'sewing', 'Sewing')",
            ).use { it.executeUpdate() }
        return mapOf("press" to 1L, "sewing" to 2L)
    }

    private fun upsertPart(
        connection: Connection,
        part: PartDto,
        lineId: Long,
    ): Long {
        val uniqNorm = normalizeUniq(part.uniq_no)
        val partNoNorm = normalizePartNumber(part.part_number)
        connection
            .prepareStatement(
                """
                INSERT INTO part(
                  uniq_no, uniq_no_norm, production_line_id,
                  part_number, part_number_norm, part_name,
                  part_number_partlist, part_name_partlist,
                  material_raw, material_note, models_source,
                  models_inferred, qty_kbn_inconsistent,
                  note_missing_in_part_requirement_list, note_missing_image_in_part_list_pdf,
                  created_at, updated_at
                ) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(uniq_no) DO UPDATE SET
                  uniq_no_norm = excluded.uniq_no_norm,
                  production_line_id = excluded.production_line_id,
                  part_number = excluded.part_number,
                  part_number_norm = excluded.part_number_norm,
                  part_name = excluded.part_name,
                  part_number_partlist = excluded.part_number_partlist,
                  part_name_partlist = excluded.part_name_partlist,
                  material_raw = excluded.material_raw,
                  material_note = excluded.material_note,
                  models_source = excluded.models_source,
                  models_inferred = excluded.models_inferred,
                  qty_kbn_inconsistent = excluded.qty_kbn_inconsistent,
                  note_missing_in_part_requirement_list = excluded.note_missing_in_part_requirement_list,
                  note_missing_image_in_part_list_pdf = excluded.note_missing_image_in_part_list_pdf,
                  updated_at = excluded.updated_at
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, part.uniq_no)
                statement.setString(2, uniqNorm)
                statement.setLong(3, lineId)
                statement.setString(4, part.part_number)
                statement.setString(5, partNoNorm)
                statement.setString(6, part.part_name)
                statement.setString(7, part.part_number_partlist)
                statement.setString(8, part.part_name_partlist)
                statement.setString(9, part.material_raw)
                statement.setString(10, part.material_note)
                statement.setString(11, part.models_source)
                statement.setInt(12, if (part.models_inferred) 1 else 0)
                statement.setInt(13, if (part.qty_kbn_inconsistent) 1 else 0)
                statement.setInt(14, if (part.notes.missing_in_part_requirement_list) 1 else 0)
                statement.setInt(15, if (part.notes.missing_image_in_part_list_pdf) 1 else 0)
                val now = Instant.now().toString()
                statement.setString(16, now)
                statement.setString(17, now)
                statement.executeUpdate()
            }
        return connection.prepareStatement("SELECT part_id FROM part WHERE uniq_no = ?").use { statement ->
            statement.setString(1, part.uniq_no)
            statement.executeQuery().use { rs ->
                rs.next()
                rs.getLong(1)
            }
        }
    }

    private fun upsertModel(
        connection: Connection,
        code: String,
    ): Long {
        connection.prepareStatement("INSERT OR IGNORE INTO model(code) VALUES(?)").use {
            it.setString(1, code)
            it.executeUpdate()
        }
        return connection.prepareStatement("SELECT model_id FROM model WHERE code = ?").use {
            it.setString(1, code)
            it.executeQuery().use { rs ->
                rs.next()
                rs.getLong(1)
            }
        }
    }

    private fun upsertTag(
        connection: Connection,
        tag: String,
    ): Long {
        connection.prepareStatement("INSERT OR IGNORE INTO material_tag(tag) VALUES(?)").use {
            it.setString(1, tag)
            it.executeUpdate()
        }
        return connection.prepareStatement("SELECT material_tag_id FROM material_tag WHERE tag = ?").use {
            it.setString(1, tag)
            it.executeQuery().use { rs ->
                rs.next()
                rs.getLong(1)
            }
        }
    }

    private fun upsertMaterial(
        connection: Connection,
        name: String,
        norm: String,
    ): Long {
        connection
            .prepareStatement(
                """
                INSERT INTO material(material_name, material_name_norm) VALUES(?, ?)
                ON CONFLICT(material_name_norm) DO UPDATE SET material_name = excluded.material_name
                """.trimIndent(),
            ).use {
                it.setString(1, name)
                it.setString(2, norm)
                it.executeUpdate()
            }
        return connection.prepareStatement("SELECT material_id FROM material WHERE material_name_norm = ?").use {
            it.setString(1, norm)
            it.executeQuery().use { rs ->
                rs.next()
                rs.getLong(1)
            }
        }
    }

    private fun syncPartModels(
        connection: Connection,
        partId: Long,
        part: PartDto,
        modelIdByCode: MutableMap<String, Long>,
    ) {
        connection.prepareStatement("DELETE FROM part_model WHERE part_id = ?").use {
            it.setLong(1, partId)
            it.executeUpdate()
        }
        part.models.map { it.trim() }.filter { it.isNotEmpty() }.distinct().forEach { modelCode ->
            val modelId = modelIdByCode.getOrPut(modelCode) { upsertModel(connection, modelCode) }
            connection.prepareStatement("INSERT INTO part_model(part_id, model_id) VALUES(?, ?)").use {
                it.setLong(1, partId)
                it.setLong(2, modelId)
                it.executeUpdate()
            }
        }
    }

    private fun syncPartRequirements(
        connection: Connection,
        partId: Long,
        part: PartDto,
        modelIdByCode: MutableMap<String, Long>,
    ) {
        connection.prepareStatement("DELETE FROM part_requirement WHERE part_id = ?").use {
            it.setLong(1, partId)
            it.executeUpdate()
        }

        val requirements = linkedMapOf<String, PartRequirementRawDto>()
        part.requirements_raw.forEach { row ->
            row.models.map { it.trim() }.filter { it.isNotEmpty() }.forEach { modelCode ->
                requirements[modelCode] = row
            }
        }
        if (requirements.isEmpty() && part.qty_kbn != null) {
            part.models.map { it.trim() }.filter { it.isNotEmpty() }.forEach { modelCode ->
                requirements[modelCode] =
                    PartRequirementRawDto(
                        part_number = part.part_number,
                        part_name = part.part_name,
                        models = listOf(modelCode),
                        qty_kbn = part.qty_kbn,
                        source_page = null,
                    )
            }
        }
        requirements.forEach { (modelCode, row) ->
            val qty = row.qty_kbn ?: return@forEach
            val modelId = modelIdByCode.getOrPut(modelCode) { upsertModel(connection, modelCode) }
            connection
                .prepareStatement(
                    """
                    INSERT INTO part_requirement(part_id, model_id, qty_kbn, source_page, source_part_number, source_part_name)
                    VALUES(?, ?, ?, ?, ?, ?)
                    ON CONFLICT(part_id, model_id) DO UPDATE SET
                      qty_kbn = excluded.qty_kbn,
                      source_page = excluded.source_page,
                      source_part_number = excluded.source_part_number,
                      source_part_name = excluded.source_part_name
                    """.trimIndent(),
                ).use { statement ->
                    statement.setLong(1, partId)
                    statement.setLong(2, modelId)
                    statement.setInt(3, qty)
                    statement.setNullableInt(4, row.source_page)
                    statement.setString(5, row.part_number)
                    statement.setString(6, row.part_name)
                    statement.executeUpdate()
                }
        }
    }

    private fun syncPartMaterials(
        connection: Connection,
        partId: Long,
        part: PartDto,
        materialIdByNorm: MutableMap<String, Long>,
        tagIdByTag: MutableMap<String, Long>,
    ) {
        connection.prepareStatement("DELETE FROM part_material_tag WHERE part_id = ?").use {
            it.setLong(1, partId)
            it.executeUpdate()
        }
        connection
            .prepareStatement(
                "DELETE FROM part_material_layer_tag WHERE part_material_layer_id IN (SELECT part_material_layer_id FROM part_material_layer WHERE part_id = ?)",
            ).use {
                it.setLong(1, partId)
                it.executeUpdate()
            }
        connection.prepareStatement("DELETE FROM part_material_layer WHERE part_id = ?").use {
            it.setLong(1, partId)
            it.executeUpdate()
        }

        part.material_tags.map { it.trim() }.filter { it.isNotEmpty() }.distinct().forEach { tag ->
            val tagId = tagIdByTag.getOrPut(tag) { upsertTag(connection, tag) }
            connection
                .prepareStatement(
                    "INSERT OR IGNORE INTO part_material_tag(part_id, material_tag_id) VALUES(?, ?)",
                ).use {
                    it.setLong(1, partId)
                    it.setLong(2, tagId)
                    it.executeUpdate()
                }
        }

        part.materials.sortedBy { it.layer_order }.forEach { layer ->
            val materialNorm = normalizeName(layer.material_name)
            if (materialNorm.isEmpty()) return@forEach
            val materialId =
                materialIdByNorm.getOrPut(materialNorm) {
                    upsertMaterial(connection, layer.material_name, materialNorm)
                }

            val layerId =
                connection
                    .prepareStatement(
                        "INSERT INTO part_material_layer(part_id, material_id, layer_order, weight_g, basis_weight_gsm, unit) VALUES(?, ?, ?, ?, ?, ?)",
                        java.sql.Statement.RETURN_GENERATED_KEYS,
                    ).use { statement ->
                        statement.setLong(1, partId)
                        statement.setLong(2, materialId)
                        statement.setInt(3, layer.layer_order)
                        statement.setNullableDouble(4, layer.weight_g)
                        statement.setNullableDouble(5, layer.basis_weight_gsm)
                        statement.setString(6, layer.unit)
                        statement.executeUpdate()
                        statement.generatedKeys.use { keys ->
                            keys.next()
                            keys.getLong(1)
                        }
                    }

            layer.layer_tags.map { it.trim() }.filter { it.isNotEmpty() }.distinct().forEach { tag ->
                val tagId = tagIdByTag.getOrPut(tag) { upsertTag(connection, tag) }
                connection
                    .prepareStatement(
                        "INSERT OR IGNORE INTO part_material_layer_tag(part_material_layer_id, material_tag_id) VALUES(?, ?)",
                    ).use {
                        it.setLong(1, layerId)
                        it.setLong(2, tagId)
                        it.executeUpdate()
                    }
            }
        }
    }

    private fun storePartImage(
        connection: Connection,
        partId: Long,
        part: PartDto,
        readImageBytes: (String) -> ByteArray?,
    ): Boolean {
        val imageBytes =
            readImageBytes(part.image.path)
                ?: if (part.image.status == "missing") transparentPngPlaceholder() else return false

        val sha = sha256Hex(imageBytes)
        val assetRef =
            runBlocking {
                assetStore.putBytes(
                    key = AssetKey(type = "part_image", uniqNo = part.uniq_no, sha256 = sha),
                    bytes = imageBytes,
                    mime = "image/png",
                )
            }
        val decoded = ImageIO.read(ByteArrayInputStream(imageBytes))
        val width = decoded?.width ?: part.image.width_px
        val height = decoded?.height ?: part.image.height_px

        connection.prepareStatement("UPDATE image_asset SET active = 0 WHERE part_id = ?").use {
            it.setLong(1, partId)
            it.executeUpdate()
        }
        connection
            .prepareStatement(
                """
                INSERT INTO image_asset(
                  part_id, status, sha256, mime, storage_relpath, size_bytes,
                  width_px, height_px, format, color_mode, transparent_background,
                  qc_alpha_border_ratio, qc_content_empty, source_json, qc_json, active, imported_at
                ) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, ?)
                ON CONFLICT(part_id, sha256) DO UPDATE SET
                  status = excluded.status,
                  mime = excluded.mime,
                  storage_relpath = excluded.storage_relpath,
                  size_bytes = excluded.size_bytes,
                  width_px = excluded.width_px,
                  height_px = excluded.height_px,
                  format = excluded.format,
                  color_mode = excluded.color_mode,
                  transparent_background = excluded.transparent_background,
                  qc_alpha_border_ratio = excluded.qc_alpha_border_ratio,
                  qc_content_empty = excluded.qc_content_empty,
                  source_json = excluded.source_json,
                  qc_json = excluded.qc_json,
                  active = 1,
                  imported_at = excluded.imported_at
                """.trimIndent(),
            ).use { statement ->
                statement.setLong(1, partId)
                statement.setString(2, part.image.status)
                statement.setString(3, assetRef.sha256)
                statement.setString(4, assetRef.mime)
                statement.setString(5, assetRef.storageRelPath)
                statement.setLong(6, assetRef.sizeBytes)
                statement.setNullableInt(7, width)
                statement.setNullableInt(8, height)
                statement.setString(9, part.image.format)
                statement.setString(10, part.image.mode)
                statement.setNullableBooleanInt(11, part.image.transparent_background)
                statement.setNullableDouble(12, part.image.qc?.alpha_border_ratio)
                statement.setNullableBooleanInt(13, part.image.qc?.content_empty)
                statement.setString(14, part.image.source?.let { json.encodeToString(it) })
                statement.setString(15, part.image.qc?.let { json.encodeToString(ImageQcDto.serializer(), it) })
                statement.setString(16, Instant.now().toString())
                statement.executeUpdate()
            }
        return true
    }

    private fun upsertQaReport(
        connection: Connection,
        report: QaReportDto,
        lineId: Long,
    ): Long {
        connection
            .prepareStatement(
                """
                INSERT INTO qa_report(report_id, report_type, production_line_id, period_year, period_month, report_date, source_file, title, imported_at)
                VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(report_id) DO UPDATE SET
                  report_type = excluded.report_type,
                  production_line_id = excluded.production_line_id,
                  period_year = excluded.period_year,
                  period_month = excluded.period_month,
                  report_date = excluded.report_date,
                  source_file = excluded.source_file,
                  title = excluded.title,
                  imported_at = excluded.imported_at
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, report.report_id)
                statement.setString(2, report.type)
                statement.setLong(3, lineId)
                statement.setNullableInt(4, report.period_year)
                statement.setNullableInt(5, report.period_month)
                statement.setString(6, report.report_date)
                statement.setString(7, report.source_file)
                statement.setString(8, report.title)
                statement.setString(9, Instant.now().toString())
                statement.executeUpdate()
            }
        return connection.prepareStatement("SELECT qa_report_id FROM qa_report WHERE report_id = ?").use { statement ->
            statement.setString(1, report.report_id)
            statement.executeQuery().use { rs ->
                rs.next()
                rs.getLong(1)
            }
        }
    }

    private fun upsertQaDefect(
        connection: Connection,
        defect: QaDefectTypeDto,
    ): Long {
        connection
            .prepareStatement(
                """
                INSERT INTO qa_defect_type(defect_type_id, defect_name, defect_name_norm)
                VALUES(?, ?, ?)
                ON CONFLICT(defect_type_id) DO UPDATE SET
                  defect_name = excluded.defect_name,
                  defect_name_norm = excluded.defect_name_norm
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, defect.defect_type_id)
                statement.setString(2, defect.name)
                statement.setString(3, defect.name_norm)
                statement.executeUpdate()
            }
        return connection
            .prepareStatement(
                "SELECT qa_defect_type_id FROM qa_defect_type WHERE defect_type_id = ?",
            ).use { statement ->
                statement.setString(1, defect.defect_type_id)
                statement.executeQuery().use { rs ->
                    rs.next()
                    rs.getLong(1)
                }
            }
    }

    private fun importDefectScreening(
        connection: Connection,
        screening: DefectScreeningDto,
        partIdByPartNumberNorm: Map<String, Long>,
        materialIdByNorm: Map<String, Long>,
    ) {
        connection.createStatement().use { it.executeUpdate("DELETE FROM part_item_defect_stat") }
        connection.createStatement().use { it.executeUpdate("DELETE FROM material_item_defect_risk") }
        connection.createStatement().use { it.executeUpdate("DELETE FROM item_defect") }

        val defectIdByNorm = mutableMapOf<String, Long>()

        fun defectId(
            defectName: String,
            line: String,
        ): Long {
            val norm = normalizeName(defectName)
            return defectIdByNorm.getOrPut(norm) {
                connection
                    .prepareStatement(
                        """
                        INSERT INTO item_defect(defect_name, defect_name_norm, source_line, source_json)
                        VALUES(?, ?, ?, NULL)
                        ON CONFLICT(defect_name_norm) DO UPDATE SET
                          defect_name = excluded.defect_name,
                          source_line = CASE
                            WHEN item_defect.source_line = excluded.source_line THEN item_defect.source_line
                            ELSE 'mixed'
                          END
                        """.trimIndent(),
                    ).use { statement ->
                        statement.setString(1, defectName.trim())
                        statement.setString(2, norm)
                        statement.setString(3, normalizeLine(line))
                        statement.executeUpdate()
                    }
                connection.prepareStatement("SELECT item_defect_id FROM item_defect WHERE defect_name_norm = ?").use {
                    it.setString(1, norm)
                    it.executeQuery().use { rs ->
                        rs.next()
                        rs.getLong(1)
                    }
                }
            }
        }

        screening.part_item_defect_stats.forEach { row ->
            val partId = partIdByPartNumberNorm[row.part_number_norm] ?: return@forEach
            val itemDefectId = defectId(row.defect_name, row.line)
            connection
                .prepareStatement(
                    """
                    INSERT INTO part_item_defect_stat(
                      part_id, item_defect_id, source_line, occurrence_qty, affected_days, last_seen_date
                    ) VALUES(?, ?, ?, ?, ?, ?)
                    ON CONFLICT(part_id, item_defect_id, source_line) DO UPDATE SET
                      occurrence_qty = excluded.occurrence_qty,
                      affected_days = excluded.affected_days,
                      last_seen_date = excluded.last_seen_date
                    """.trimIndent(),
                ).use { statement ->
                    statement.setLong(1, partId)
                    statement.setLong(2, itemDefectId)
                    statement.setString(3, normalizeLine(row.line))
                    statement.setInt(4, row.occurrence_qty.coerceAtLeast(0))
                    statement.setInt(5, row.affected_days.coerceAtLeast(0))
                    statement.setString(6, row.last_seen_sheet)
                    statement.executeUpdate()
                }
        }

        screening.material_item_defect_risk.forEach { row ->
            val materialId = materialIdByNorm[row.material_name_norm] ?: return@forEach
            val itemDefectId = defectId(row.defect_name, row.line)
            connection
                .prepareStatement(
                    """
                    INSERT INTO material_item_defect_risk(
                      material_id, item_defect_id, source_line, risk_score, affected_parts, sample_size
                    ) VALUES(?, ?, ?, ?, ?, ?)
                    ON CONFLICT(material_id, item_defect_id, source_line) DO UPDATE SET
                      risk_score = excluded.risk_score,
                      affected_parts = excluded.affected_parts,
                      sample_size = excluded.sample_size
                    """.trimIndent(),
                ).use { statement ->
                    statement.setLong(1, materialId)
                    statement.setLong(2, itemDefectId)
                    statement.setString(3, normalizeLine(row.line))
                    statement.setDouble(4, row.risk_score.coerceAtLeast(0.0))
                    statement.setInt(5, row.affected_parts.coerceAtLeast(0))
                    statement.setInt(6, row.sample_size.coerceAtLeast(0))
                    statement.executeUpdate()
                }
        }
    }
}

private fun normalizeUniq(value: String): String = value.trim().uppercase().replace("\\s+".toRegex(), "")

private fun normalizePartNumber(value: String): String =
    value
        .trim()
        .uppercase()
        .replace("O", "0")
        .replace("[^A-Z0-9]".toRegex(), "")

private fun normalizeName(value: String): String = value.trim().uppercase().replace("\\s+".toRegex(), " ")

private fun normalizeLine(value: String): String {
    val text = value.trim().lowercase()
    return when {
        text.contains("press") -> "press"
        text.contains("sew") -> "sewing"
        else -> "mixed"
    }
}

private fun transparentPngPlaceholder(): ByteArray {
    val image = java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_ARGB)
    val out = java.io.ByteArrayOutputStream()
    ImageIO.write(image, "png", out)
    return out.toByteArray()
}

private fun sha256Hex(bytes: ByteArray): String =
    MessageDigest
        .getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { "%02x".format(it) }

private fun java.sql.PreparedStatement.setNullableInt(
    index: Int,
    value: Int?,
) {
    if (value == null) {
        setNull(index, Types.INTEGER)
    } else {
        setInt(index, value)
    }
}

private fun java.sql.PreparedStatement.setNullableDouble(
    index: Int,
    value: Double?,
) {
    if (value == null) {
        setNull(index, Types.DOUBLE)
    } else {
        setDouble(index, value)
    }
}

private fun java.sql.PreparedStatement.setNullableBooleanInt(
    index: Int,
    value: Boolean?,
) {
    if (value == null) {
        setNull(index, Types.INTEGER)
    } else {
        setInt(index, if (value) 1 else 0)
    }
}
