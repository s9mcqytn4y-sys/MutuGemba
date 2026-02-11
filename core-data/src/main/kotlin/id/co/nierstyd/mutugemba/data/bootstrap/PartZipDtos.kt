package id.co.nierstyd.mutugemba.data.bootstrap

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class MappingRootDto(
    val generated_at: String? = null,
    val indexes: IndexesDto = IndexesDto(),
    val qa: QaDto = QaDto(),
    val parts: List<PartDto> = emptyList(),
)

@Serializable
data class IndexesDto(
    val models: List<String> = emptyList(),
    val material_tags: List<String> = emptyList(),
)

@Serializable
data class QaDto(
    val reports: List<QaReportDto> = emptyList(),
    val defect_types: List<QaDefectTypeDto> = emptyList(),
    val report_legends: List<QaReportLegendDto> = emptyList(),
    val part_summaries: List<QaPartSummaryDto> = emptyList(),
    val observations: List<QaObservationDto> = emptyList(),
)

@Serializable
data class QaReportDto(
    val report_id: String,
    val type: String,
    val line: String,
    val period_year: Int? = null,
    val period_month: Int? = null,
    val report_date: String? = null,
    val source_file: String? = null,
    val title: String? = null,
)

@Serializable
data class QaDefectTypeDto(
    val defect_type_id: String,
    val name: String,
    val name_norm: String,
)

@Serializable
data class QaReportLegendDto(
    val report_id: String,
    val code: String,
    val defect_type_id: String,
    val defect_name: String? = null,
)

@Serializable
data class QaPartSummaryDto(
    val summary_id: String,
    val report_id: String,
    val uniq_no: String,
    val total_check: Int? = null,
    val total_ok: Int? = null,
    val total_defect: Int? = null,
    val source: String? = null,
)

@Serializable
data class QaObservationDto(
    val observation_id: String,
    val report_id: String,
    val uniq_no: String,
    val part_number_in_report: String? = null,
    val defect_type_id: String,
    val defect_name: String? = null,
    val qty: Int = 0,
    val source: String? = null,
)

@Serializable
data class PartDto(
    val uniq_no: String,
    val production_line: String,
    val part_number: String,
    val part_name: String = "",
    val part_number_partlist: String? = null,
    val part_name_partlist: String? = null,
    val material_raw: String? = null,
    val material_note: String? = null,
    val models: List<String> = emptyList(),
    val qty_kbn: Int? = null,
    val qty_kbn_inconsistent: Boolean = false,
    val models_inferred: Boolean = false,
    val models_source: String = "",
    val requirements_raw: List<PartRequirementRawDto> = emptyList(),
    val image: ImageDto,
    val notes: PartNoteDto = PartNoteDto(),
    val materials: List<PartMaterialLayerDto> = emptyList(),
    val material_tags: List<String> = emptyList(),
)

@Serializable
data class PartRequirementRawDto(
    val part_number: String? = null,
    val part_name: String? = null,
    val models: List<String> = emptyList(),
    val qty_kbn: Int? = null,
    val source_page: Int? = null,
)

@Serializable
data class PartNoteDto(
    val missing_in_part_requirement_list: Boolean = false,
    val missing_image_in_part_list_pdf: Boolean = false,
)

@Serializable
data class ImageDto(
    val status: String,
    val path: String,
    val sha256: String,
    val width_px: Int? = null,
    val height_px: Int? = null,
    val format: String? = null,
    val mode: String? = null,
    val transparent_background: Boolean? = null,
    val source: JsonElement? = null,
    val qc: ImageQcDto? = null,
)

@Serializable
data class ImageQcDto(
    val alpha_border_ratio: Double? = null,
    val content_empty: Boolean? = null,
    val rendered_page_size_px: List<Int> = emptyList(),
)

@Serializable
data class PartMaterialLayerDto(
    val layer_order: Int,
    val material_name: String,
    val weight_g: Double? = null,
    val basis_weight_gsm: Double? = null,
    val unit: String? = null,
    val layer_tags: List<String> = emptyList(),
)

@Serializable
data class DefectScreeningDto(
    val generated_at: String? = null,
    val summary: DefectScreeningSummaryDto = DefectScreeningSummaryDto(),
    val part_item_defect_stats: List<PartItemDefectStatDto> = emptyList(),
    val material_item_defect_risk: List<MaterialItemDefectRiskDto> = emptyList(),
)

@Serializable
data class DefectScreeningSummaryDto(
    val total_observations: Int = 0,
    val distinct_part_defect_pairs: Int = 0,
    val distinct_material_defect_risk: Int = 0,
)

@Serializable
data class PartItemDefectStatDto(
    val line: String,
    val part_number: String,
    val part_number_norm: String,
    val defect_name: String,
    val defect_name_norm: String,
    val occurrence_qty: Int,
    val affected_days: Int,
    val last_seen_sheet: String? = null,
)

@Serializable
data class MaterialItemDefectRiskDto(
    val line: String,
    val material_name: String,
    val material_name_norm: String,
    val defect_name: String,
    val defect_name_norm: String,
    val risk_score: Double,
    val affected_parts: Int,
    val sample_size: Int,
)
