package id.co.nierstyd.mutugemba.data.db

import id.co.nierstyd.mutugemba.data.AppDataPaths
import id.co.nierstyd.mutugemba.data.bootstrap.DefectScreeningDto
import id.co.nierstyd.mutugemba.data.bootstrap.MappingRootDto
import id.co.nierstyd.mutugemba.domain.DefectNameSanitizer
import id.co.nierstyd.mutugemba.domain.DefectSeverity
import id.co.nierstyd.mutugemba.domain.DefectType
import id.co.nierstyd.mutugemba.domain.InspectionDefectEntry
import id.co.nierstyd.mutugemba.domain.InspectionInput
import id.co.nierstyd.mutugemba.domain.InspectionRecord
import id.co.nierstyd.mutugemba.domain.Line
import id.co.nierstyd.mutugemba.domain.LineCode
import id.co.nierstyd.mutugemba.domain.Part
import id.co.nierstyd.mutugemba.domain.Shift
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicLong

data class StoredInspection(
    val input: InspectionInput,
    val record: InspectionRecord,
    val createdDate: LocalDate,
    val defects: List<InspectionDefectEntry>,
)

class InMemoryDatabase(
    val databaseFile: Path,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val idGenerator = AtomicLong(0)
    private val extractedRoot = AppDataPaths.defaultPartAssetsExtractedDir()
    private val mapping = loadMapping(extractedRoot)
    private val screening = loadScreening(extractedRoot)

    val lines: List<Line> =
        listOf(
            Line(id = 1L, code = LineCode.PRESS, name = "Press"),
            Line(id = 2L, code = LineCode.SEWING, name = "Sewing"),
        )

    val shifts: List<Shift> =
        listOf(
            Shift(id = 1L, code = "S1", name = "Shift 1 (08:00-17:00 WIB)", startTime = "08:00", endTime = "17:00"),
        )

    private val defectSpecs: List<DefectSpec> = buildDefectSpecs()
    private val defectNameByCode: Map<String, String> = defectSpecs.associate { it.code to it.name }
    private val defectFrequencyByCode: Map<String, Int> = buildDefectFrequencyByCode()
    private val materialRiskCodesByMaterialNorm: Map<String, List<String>> = buildMaterialRiskCodesByMaterialNorm()
    private val partRiskCodesByPartNumberNorm: Map<String, List<String>> = buildPartRiskCodesByPartNumberNorm()
    private val defectCodesByLine: Map<LineCode, Set<String>> = buildDefectCodesByLine()

    private val baseDefectTypes: List<DefectType> =
        defectSpecs.mapIndexed { index, spec ->
            DefectType(
                id = index + 1L,
                code = spec.code,
                name = spec.name,
                category = if (spec.name.isProcessNgName()) "NG_PROSES" else "NG_MATERIAL",
                severity = DefectSeverity.NORMAL,
                lineCode =
                    when (spec.lineCode) {
                        "sewing" -> LineCode.SEWING
                        "press" -> LineCode.PRESS
                        else -> null
                    },
            )
        }
    private val customDefectTypes = mutableListOf<DefectType>()
    private val nextDefectId = AtomicLong((baseDefectTypes.maxOfOrNull { it.id } ?: 0L) + 1L)

    val defectTypes: List<DefectType>
        get() = baseDefectTypes + customDefectTypes

    private val customDefectCodesByLine = mutableMapOf<LineCode, MutableSet<String>>()
    private val baseDefectLineByCode = baseDefectTypes.associate { it.code to it.lineCode }

    private fun defectCodeToIdMap(): Map<String, Long> = defectTypes.associateBy({ it.code }, { it.id })

    val parts: List<Part> =
        mapping.parts.mapIndexedNotNull { index, part ->
            val lineCode =
                if (part.production_line.equals("sewing", ignoreCase = true)) {
                    LineCode.SEWING
                } else {
                    LineCode.PRESS
                }
            // Part line press dengan material strap tidak ditampilkan di checksheet input
            // karena material tersebut milik client dan tidak masuk tracking kerugian internal.
            val materialComposite = listOfNotNull(part.material_raw, part.material_note).joinToString(" ")
            if (lineCode == LineCode.PRESS && materialComposite.contains("strap", ignoreCase = true)) {
                return@mapIndexedNotNull null
            }
            val partNumberNorm = normalizePartNumber(part.part_number)
            val recommendedCodes = recommendedDefectCodesForPart(part, partNumberNorm, lineCode)
            Part(
                id = index + 1L,
                partNumber = part.part_number,
                model = part.models.firstOrNull() ?: "-",
                name = part.part_name.ifBlank { part.part_name_partlist.orEmpty() }.ifBlank { "-" },
                uniqCode = part.uniq_no,
                material = part.material_raw ?: part.material_note ?: "-",
                picturePath = resolvePartImagePath(part.image.sha256, part.image.path),
                lineCode = lineCode,
                recommendedDefectCodes = recommendedCodes,
            )
        }

    val inspections = mutableListOf<StoredInspection>()

    fun toRecord(input: InspectionInput): InspectionRecord {
        val line = lines.firstOrNull { it.id == input.lineId }
        val shift = shifts.firstOrNull { it.id == input.shiftId }
        val part = parts.firstOrNull { it.id == input.partId }

        return InspectionRecord(
            id = idGenerator.incrementAndGet(),
            kind = input.kind,
            lineName = line?.name ?: "-",
            shiftName = shift?.name ?: "-",
            partName = part?.name ?: "-",
            partNumber = part?.partNumber ?: "-",
            totalCheck = input.totalCheck,
            createdAt = input.createdAt,
        )
    }

    fun clearAll() {
        inspections.clear()
        idGenerator.set(0)
    }

    private fun recommendedDefectCodesForPart(
        part: id.co.nierstyd.mutugemba.data.bootstrap.PartDto,
        partNumberNorm: String,
        lineCode: LineCode,
    ): List<String> {
        val materialNorms =
            buildList {
                part.materials.mapTo(this) { normalizeName(it.material_name) }
                tokenizeMaterial(part.material_raw).mapTo(this) { normalizeName(it) }
                tokenizeMaterial(part.material_note).mapTo(this) { normalizeName(it) }
            }.filter { it.isNotBlank() }

        val fromPartScreening = partRiskCodesByPartNumberNorm[partNumberNorm].orEmpty()
        val fromMaterial = materialNorms.flatMap { materialRiskCodesByMaterialNorm[it].orEmpty() }
        val byLine = defectCodesByLine[lineCode].orEmpty().toList()
        val fallback = baseDefectTypes.map { it.code }

        val candidates =
            listOf(fromPartScreening, fromMaterial, byLine, fallback)
                .firstOrNull { it.isNotEmpty() }
                .orEmpty()

        val compatible =
            candidates.filter { defectCode ->
                isDefectCompatible(
                    lineCode = lineCode,
                    materialNorms = materialNorms,
                    defectCode = defectCode,
                )
            }
        return sortDefectCodesSmart(compatible)
    }

    private fun buildDefectSpecs(): List<DefectSpec> {
        val byCode = linkedMapOf<String, DefectSpec>()

        screening
            ?.part_item_defect_stats
            .orEmpty()
            .forEach { row ->
                val normalizedCode = normalizeName(row.defect_name_norm.ifBlank { row.defect_name })
                val displayName = cleanDefectName(row.defect_name)
                if (!isDefectNameValid(normalizedCode, displayName)) return@forEach
                byCode.putIfAbsent(
                    normalizedCode,
                    DefectSpec(
                        code = normalizedCode,
                        name = displayName,
                        lineCode = normalizeLine(row.line),
                    ),
                )
            }

        mapping.qa.defect_types.forEach { row ->
            val normalizedCode = normalizeName(row.name_norm.ifBlank { row.name })
            val displayName = cleanDefectName(row.name)
            if (!isDefectNameValid(normalizedCode, displayName)) return@forEach
            byCode.putIfAbsent(
                normalizedCode,
                DefectSpec(
                    code = normalizedCode,
                    name = displayName,
                    lineCode = null,
                ),
            )
        }

        return byCode.values.toList()
    }

    private fun buildPartRiskCodesByPartNumberNorm(): Map<String, List<String>> =
        screening
            ?.part_item_defect_stats
            .orEmpty()
            .groupBy { it.part_number_norm }
            .mapValues { (_, rows) ->
                rows
                    .sortedByDescending { it.occurrence_qty }
                    .map { normalizeName(it.defect_name_norm.ifBlank { it.defect_name }) }
                    .filter { it.isNotBlank() }
                    .distinct()
            }

    private fun buildDefectFrequencyByCode(): Map<String, Int> =
        screening
            ?.part_item_defect_stats
            .orEmpty()
            .groupBy { normalizeName(it.defect_name_norm.ifBlank { it.defect_name }) }
            .mapValues { (_, rows) -> rows.sumOf { it.occurrence_qty } }

    private fun buildMaterialRiskCodesByMaterialNorm(): Map<String, List<String>> =
        screening
            ?.material_item_defect_risk
            .orEmpty()
            .groupBy { it.material_name_norm }
            .mapValues { (_, rows) ->
                rows
                    .sortedByDescending { it.risk_score }
                    .map { normalizeName(it.defect_name_norm.ifBlank { it.defect_name }) }
                    .filter { it.isNotBlank() }
                    .distinct()
            }

    private fun buildDefectCodesByLine(): Map<LineCode, Set<String>> {
        if (screening == null) {
            val all = defectSpecs.map { it.code }.toSet()
            return mapOf(LineCode.PRESS to all, LineCode.SEWING to all)
        }
        val grouped =
            screening.part_item_defect_stats.groupBy { normalizeLine(it.line) }.mapValues { (_, rows) ->
                rows
                    .map { normalizeName(it.defect_name_norm.ifBlank { it.defect_name }) }
                    .filter { it.isNotBlank() }
                    .toSet()
            }
        return mapOf(
            LineCode.PRESS to grouped["press"].orEmpty().ifEmpty { grouped["sewing"].orEmpty() },
            LineCode.SEWING to grouped["sewing"].orEmpty().ifEmpty { grouped["press"].orEmpty() },
        )
    }

    private fun resolvePartImagePath(
        sha256: String,
        mappingImagePath: String,
    ): String? {
        val normalizedSha = sha256.trim().lowercase()
        if (normalizedSha.length == 64) {
            val hashPath =
                AppDataPaths
                    .assetsStoreDir()
                    .resolve("images")
                    .resolve("sha256")
                    .resolve(normalizedSha.take(2))
                    .resolve("$normalizedSha.png")
            if (Files.exists(hashPath)) {
                return hashPath.toAbsolutePath().normalize().toString()
            }
        }

        val extractedPath = extractedRoot.resolve(mappingImagePath.replace('/', java.io.File.separatorChar))
        if (Files.exists(extractedPath)) {
            return extractedPath.toAbsolutePath().normalize().toString()
        }
        return null
    }

    fun recommendedDefectTypeIds(partId: Long): Set<Long> {
        val part = parts.firstOrNull { it.id == partId } ?: return emptySet()
        val defectCodeToId = defectCodeToIdMap()
        val ids = part.recommendedDefectCodes.mapNotNull { defectCodeToId[it] }.toSet()
        return if (ids.isNotEmpty()) ids else defectTypes.map { it.id }.toSet()
    }

    @Synchronized
    fun upsertDefectType(
        name: String,
        lineCode: LineCode,
    ): DefectType {
        val normalizedCode = normalizeName(name)
        require(normalizedCode.isNotBlank()) { "Defect name cannot be blank" }

        val existing = defectTypes.firstOrNull { it.code == normalizedCode || it.name.equals(normalizedCode, true) }
        if (existing != null) {
            return existing
        }

        val created =
            DefectType(
                id = nextDefectId.getAndIncrement(),
                code = normalizedCode,
                name = cleanDefectName(name),
                category = "CUSTOM",
                severity = DefectSeverity.NORMAL,
                lineCode = lineCode,
            )
        customDefectTypes += created
        customDefectCodesByLine.getOrPut(lineCode) { linkedSetOf() }.add(created.code)
        return created
    }

    private fun sortDefectCodesSmart(codes: List<String>): List<String> =
        codes
            .map(::normalizeName)
            .filter { it.isNotBlank() }
            .distinct()
            .sortedWith(
                compareByDescending<String> { defectFrequencyByCode[it] ?: 0 }
                    .thenBy { defectNameByCode[it] ?: it },
            )

    private fun isDefectCompatible(
        lineCode: LineCode,
        materialNorms: List<String>,
        defectCode: String,
    ): Boolean {
        val normalizedDefect = normalizeName(defectCode)
        if (normalizedDefect.isBlank()) return false

        val declaredLine = baseDefectLineByCode[normalizedDefect]
        val customLineMatch = customDefectCodesByLine[lineCode]?.contains(normalizedDefect) == true
        if (declaredLine != null && declaredLine != lineCode && !customLineMatch) {
            return false
        }

        val materialText = materialNorms.joinToString(" ")
        val isCarpet = materialText.contains("CARPET")
        if (isCarpet && normalizedDefect.contains("LAMINATING")) {
            return false
        }
        if (lineCode == LineCode.PRESS && normalizedDefect.contains("SEWING")) {
            return false
        }
        return true
    }

    private fun loadMapping(extractedDir: Path): MappingRootDto {
        val mappingPath = extractedDir.resolve("mappings").resolve("mapping.json")
        if (!Files.exists(mappingPath)) {
            return MappingRootDto()
        }
        return runCatching {
            val payload = Files.readString(mappingPath)
            json.decodeFromString(MappingRootDto.serializer(), payload)
        }.getOrElse { MappingRootDto() }
    }

    private fun loadScreening(extractedDir: Path): DefectScreeningDto? {
        val screeningPath = extractedDir.resolve("reports").resolve("defect_screening.json")
        if (!Files.exists(screeningPath)) {
            return null
        }
        return runCatching {
            val payload = Files.readString(screeningPath)
            json.decodeFromString(DefectScreeningDto.serializer(), payload)
        }.getOrNull()
    }

    private fun tokenizeMaterial(raw: String?): List<String> =
        raw
            .orEmpty()
            .split(",", ";", "|", "/")
            .map { it.trim() }
            .filter { it.isNotBlank() }

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

    private fun cleanDefectName(value: String): String =
        DefectNameSanitizer.normalizeDisplay(value).ifBlank { normalizeName(value) }

    private fun isDefectNameValid(
        code: String,
        displayName: String,
    ): Boolean {
        if (code.isBlank()) return false
        if (displayName.length < 3) return false
        val invalidTokens = setOf("A", "-", "--", ".", "TOTAL", "SUBTOTAL", "SUB-TOTAL")
        if (code in invalidTokens) return false
        if (displayName.uppercase() in invalidTokens) return false
        return true
    }
}

private data class DefectSpec(
    val code: String,
    val name: String,
    val lineCode: String?,
)

private fun String.isProcessNgName(): Boolean {
    val normalized = uppercase()
    val processKeywords =
        listOf(
            "OVERCUTTING",
            "MIRING",
            "TERBALIK",
            "MARGIN",
            "SALAH",
            "MISS",
            "SHIFT",
            "HUMAN",
        )
    return processKeywords.any { normalized.contains(it) }
}
