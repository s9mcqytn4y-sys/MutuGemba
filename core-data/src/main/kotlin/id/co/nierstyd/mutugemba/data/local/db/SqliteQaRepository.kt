package id.co.nierstyd.mutugemba.data.local.db

import id.co.nierstyd.mutugemba.domain.model.DefectHeatmapCell
import id.co.nierstyd.mutugemba.domain.model.ModelDefectTop
import id.co.nierstyd.mutugemba.domain.repository.QARepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SqliteQaRepository(
    private val database: SqliteDatabase,
) : QARepository {
    override suspend fun getTopDefectsPerModelMonthly(
        year: Int,
        month: Int,
        topN: Int,
    ): List<ModelDefectTop> =
        withContext(Dispatchers.IO) {
            database.read { connection ->
                connection
                    .prepareStatement(
                        """
                        WITH model_month_defect AS (
                          SELECT
                            m.code AS model_code,
                            qr.period_year,
                            qr.period_month,
                            qdt.defect_type_id,
                            qdt.defect_name,
                            SUM(qo.qty) AS total_qty
                          FROM qa_part_defect_observation qo
                          JOIN qa_report qr ON qr.qa_report_id = qo.qa_report_id
                          JOIN qa_defect_type qdt ON qdt.qa_defect_type_id = qo.qa_defect_type_id
                          JOIN part_model pm ON pm.part_id = qo.part_id
                          JOIN model m ON m.model_id = pm.model_id
                          WHERE qr.period_year = ?
                            AND qr.period_month = ?
                          GROUP BY m.code, qr.period_year, qr.period_month, qdt.defect_type_id, qdt.defect_name
                        ),
                        ranked AS (
                          SELECT
                            model_code,
                            period_year,
                            period_month,
                            defect_type_id,
                            defect_name,
                            total_qty,
                            ROW_NUMBER() OVER (
                              PARTITION BY model_code, period_year, period_month
                              ORDER BY total_qty DESC, defect_name ASC
                            ) AS rn
                          FROM model_month_defect
                        )
                        SELECT
                          model_code,
                          period_year,
                          period_month,
                          defect_type_id,
                          defect_name,
                          total_qty
                        FROM ranked
                        WHERE rn <= ?
                        ORDER BY model_code, period_year DESC, period_month DESC, total_qty DESC
                        """.trimIndent(),
                    ).use { statement ->
                        statement.setInt(1, year)
                        statement.setInt(2, month)
                        statement.setInt(3, topN)
                        statement.executeQuery().use { rs ->
                            buildList {
                                while (rs.next()) {
                                    add(
                                        ModelDefectTop(
                                            modelCode = rs.getString("model_code"),
                                            periodYear = rs.getInt("period_year"),
                                            periodMonth = rs.getInt("period_month"),
                                            defectTypeId = rs.getString("defect_type_id"),
                                            defectName = rs.getString("defect_name"),
                                            totalQty = rs.getInt("total_qty"),
                                        ),
                                    )
                                }
                            }
                        }
                    }
            }
        }

    override suspend fun getDefectHeatmap(
        year: Int,
        month: Int,
        modelCode: String?,
    ): List<DefectHeatmapCell> =
        withContext(Dispatchers.IO) {
            database.read { connection ->
                connection
                    .prepareStatement(
                        """
                        SELECT
                          qr.report_date AS report_date,
                          qdt.defect_type_id,
                          qdt.defect_name,
                          SUM(qo.qty) AS total_qty
                        FROM qa_part_defect_observation qo
                        JOIN qa_report qr ON qr.qa_report_id = qo.qa_report_id
                        JOIN qa_defect_type qdt ON qdt.qa_defect_type_id = qo.qa_defect_type_id
                        JOIN part_model pm ON pm.part_id = qo.part_id
                        JOIN model m ON m.model_id = pm.model_id
                        WHERE qr.period_year = ?
                          AND qr.period_month = ?
                          AND (? IS NULL OR m.code = ?)
                        GROUP BY qr.report_date, qdt.defect_type_id, qdt.defect_name
                        ORDER BY qr.report_date ASC, total_qty DESC
                        """.trimIndent(),
                    ).use { statement ->
                        statement.setInt(1, year)
                        statement.setInt(2, month)
                        statement.setString(3, modelCode)
                        statement.setString(4, modelCode)
                        statement.executeQuery().use { rs ->
                            buildList {
                                while (rs.next()) {
                                    add(
                                        DefectHeatmapCell(
                                            reportDate = rs.getString("report_date"),
                                            defectTypeId = rs.getString("defect_type_id"),
                                            defectName = rs.getString("defect_name"),
                                            totalQty = rs.getInt("total_qty"),
                                        ),
                                    )
                                }
                            }
                        }
                    }
            }
        }
}
