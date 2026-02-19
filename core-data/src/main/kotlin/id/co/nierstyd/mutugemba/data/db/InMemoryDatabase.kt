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
import id.co.nierstyd.mutugemba.domain.InspectionTimeSlot
import id.co.nierstyd.mutugemba.domain.Line
import id.co.nierstyd.mutugemba.domain.LineCode
import id.co.nierstyd.mutugemba.domain.Part
import id.co.nierstyd.mutugemba.domain.Shift
import kotlinx.serialization.Serializable
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
    private val stateFile: Path,
) {
    val stateFilePath: Path
        get() = stateFile

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
    private var bulkMutationDepth: Int = 0
    private var pendingFlush: Boolean = false

    init {
        loadState()
    }

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

    @Synchronized
    fun insertInspection(input: InspectionInput): InspectionRecord {
        val record = toRecord(input)
        val createdDate = parseCreatedDate(input.createdAt)
        val defects = normalizeDefects(input)
        inspections +=
            StoredInspection(
                input = input,
                record = record,
                createdDate = createdDate,
                defects = defects,
            )
        markDirtyAndMaybePersist()
        return record
    }

    fun clearAll() {
        inspections.clear()
        idGenerator.set(0)
        markDirtyAndMaybePersist()
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
        val byName = linkedMapOf<String, DefectSpec>()

        fun register(
            rawCode: String,
            rawName: String,
            rawLine: String?,
        ) {
            val displayName = cleanDefectName(rawName)
            val canonicalName = DefectNameSanitizer.canonicalKey(displayName)
            if (!isDefectNameValid(rawCode, displayName)) return
            if (canonicalName.isBlank()) return
            val normalizedCode = normalizeName(canonicalName)
            val existing = byName[canonicalName]
            val nextLine = normalizeLine(rawLine.orEmpty()).takeIf { it != "mixed" }
            if (existing == null) {
                byName[canonicalName] =
                    DefectSpec(
                        code = normalizedCode,
                        name = displayName,
                        lineCode = nextLine,
                    )
            } else if (existing.lineCode == null && nextLine != null) {
                byName[canonicalName] =
                    existing.copy(
                        code = normalizedCode,
                        name = displayName,
                        lineCode = nextLine,
                    )
            }
        }

        screening
            ?.part_item_defect_stats
            .orEmpty()
            .forEach { row ->
                register(
                    rawCode = row.defect_name_norm.ifBlank { row.defect_name },
                    rawName = row.defect_name,
                    rawLine = row.line,
                )
            }

        mapping.qa.defect_types.forEach { row ->
            register(
                rawCode = row.name_norm.ifBlank { row.name },
                rawName = row.name,
                rawLine = null,
            )
        }

        return byName.values.toList()
    }

    private fun buildPartRiskCodesByPartNumberNorm(): Map<String, List<String>> =
        screening
            ?.part_item_defect_stats
            .orEmpty()
            .groupBy { it.part_number_norm }
            .mapValues { (_, rows) ->
                rows
                    .sortedByDescending { it.occurrence_qty }
                    .map { normalizeDefectCode(it.defect_name_norm.ifBlank { it.defect_name }) }
                    .filter { it.isNotBlank() }
                    .distinct()
            }

    private fun buildDefectFrequencyByCode(): Map<String, Int> =
        screening
            ?.part_item_defect_stats
            .orEmpty()
            .groupBy { normalizeDefectCode(it.defect_name_norm.ifBlank { it.defect_name }) }
            .mapValues { (_, rows) -> rows.sumOf { it.occurrence_qty } }

    private fun buildMaterialRiskCodesByMaterialNorm(): Map<String, List<String>> =
        screening
            ?.material_item_defect_risk
            .orEmpty()
            .groupBy { it.material_name_norm }
            .mapValues { (_, rows) ->
                rows
                    .sortedByDescending { it.risk_score }
                    .map { normalizeDefectCode(it.defect_name_norm.ifBlank { it.defect_name }) }
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
                    .map { normalizeDefectCode(it.defect_name_norm.ifBlank { it.defect_name }) }
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
        val normalizedCode = normalizeDefectCode(name)
        require(normalizedCode.isNotBlank()) { "Defect name cannot be blank" }

        val existing =
            defectTypes.firstOrNull { defect ->
                defect.code == normalizedCode ||
                    DefectNameSanitizer.canonicalKey(defect.name) == DefectNameSanitizer.canonicalKey(name)
            }
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
        markDirtyAndMaybePersist()
        return created
    }

    @Synchronized
    fun <T> withBulkMutation(block: () -> T): T {
        bulkMutationDepth += 1
        return try {
            block()
        } finally {
            bulkMutationDepth = (bulkMutationDepth - 1).coerceAtLeast(0)
            if (bulkMutationDepth == 0 && pendingFlush) {
                persistState()
                pendingFlush = false
            }
        }
    }

    private fun parseCreatedDate(raw: String): LocalDate =
        runCatching {
            java.time.LocalDateTime
                .parse(raw)
                .toLocalDate()
        }.getOrDefault(LocalDate.now())

    private fun normalizeDefects(input: InspectionInput): List<InspectionDefectEntry> =
        if (input.defects.isNotEmpty()) {
            input.defects
        } else {
            val defectTypeId = input.defectTypeId
            val defectQuantity = input.defectQuantity
            if (defectTypeId != null && defectQuantity != null && defectQuantity > 0) {
                listOf(InspectionDefectEntry(defectTypeId, defectQuantity))
            } else {
                emptyList()
            }
        }

    @Synchronized
    private fun loadState() {
        if (!Files.exists(stateFile)) return
        val state =
            runCatching {
                json.decodeFromString(LocalStatePayload.serializer(), Files.readString(stateFile))
            }.getOrNull() ?: return

        val customByCode = baseDefectTypes.associateBy { it.code } + customDefectTypes.associateBy { it.code }
        state.customDefects.forEach { saved ->
            if (customByCode.containsKey(saved.code)) return@forEach
            customDefectTypes +=
                DefectType(
                    id = saved.id,
                    code = saved.code,
                    name = saved.name,
                    category = "CUSTOM",
                    severity = DefectSeverity.NORMAL,
                    lineCode =
                        when (saved.lineCode) {
                            LineCode.PRESS.name -> LineCode.PRESS
                            LineCode.SEWING.name -> LineCode.SEWING
                            else -> null
                        },
                )
        }

        inspections.clear()
        inspections +=
            state.inspections.map { saved ->
                val defects =
                    saved.defects.map { savedDefect ->
                        InspectionDefectEntry(
                            defectTypeId = savedDefect.defectTypeId,
                            quantity = savedDefect.quantity,
                            slots =
                                savedDefect.slots.map { slot ->
                                    id.co.nierstyd.mutugemba.domain.InspectionDefectSlot(
                                        slot = InspectionTimeSlot.fromCode(slot.slotCode),
                                        quantity = slot.quantity,
                                    )
                                },
                        )
                    }
                val input =
                    InspectionInput(
                        kind =
                            id.co.nierstyd.mutugemba.domain.InspectionKind
                                .valueOf(saved.kind),
                        lineId = saved.lineId,
                        shiftId = saved.shiftId,
                        partId = saved.partId,
                        totalCheck = saved.totalCheck,
                        defectTypeId = null,
                        defectQuantity = null,
                        defects = defects,
                        picName = saved.picName,
                        createdAt = saved.createdAt,
                    )
                val record =
                    InspectionRecord(
                        id = saved.recordId,
                        kind = input.kind,
                        lineName = lines.firstOrNull { it.id == input.lineId }?.name ?: "-",
                        shiftName = shifts.firstOrNull { it.id == input.shiftId }?.name ?: "-",
                        partName = parts.firstOrNull { it.id == input.partId }?.name ?: "-",
                        partNumber = parts.firstOrNull { it.id == input.partId }?.partNumber ?: "-",
                        totalCheck = saved.totalCheck,
                        createdAt = saved.createdAt,
                    )
                StoredInspection(
                    input = input,
                    record = record,
                    createdDate = parseCreatedDate(saved.createdAt),
                    defects = defects,
                )
            }

        val maxInspectionId = inspections.maxOfOrNull { it.record.id } ?: 0L
        val maxDefectId = (baseDefectTypes + customDefectTypes).maxOfOrNull { it.id } ?: 0L
        idGenerator.set(maxInspectionId)
        nextDefectId.set(maxDefectId + 1L)
    }

    @Synchronized
    private fun persistState() {
        runCatching {
            Files.createDirectories(stateFile.parent)
            val payload =
                LocalStatePayload(
                    inspections =
                        inspections.map { row ->
                            SavedInspection(
                                recordId = row.record.id,
                                kind = row.input.kind.name,
                                lineId = row.input.lineId,
                                shiftId = row.input.shiftId,
                                partId = row.input.partId,
                                totalCheck = row.input.totalCheck,
                                picName = row.input.picName,
                                createdAt = row.input.createdAt,
                                defects =
                                    row.defects.map { defect ->
                                        SavedDefectEntry(
                                            defectTypeId = defect.defectTypeId,
                                            quantity = defect.totalQuantity,
                                            slots =
                                                defect.slots.map { slot ->
                                                    SavedDefectSlot(
                                                        slotCode = slot.slot.code,
                                                        quantity = slot.quantity,
                                                    )
                                                },
                                        )
                                    },
                            )
                        },
                    customDefects =
                        customDefectTypes.map { defect ->
                            SavedCustomDefect(
                                id = defect.id,
                                code = defect.code,
                                name = defect.name,
                                lineCode = defect.lineCode?.name,
                            )
                        },
                )
            Files.writeString(stateFile, json.encodeToString(LocalStatePayload.serializer(), payload))
        }
    }

    @Synchronized
    private fun markDirtyAndMaybePersist() {
        if (bulkMutationDepth > 0) {
            pendingFlush = true
            return
        }
        persistState()
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

    private fun normalizeDefectCode(value: String): String =
        normalizeName(
            DefectNameSanitizer.canonicalKey(
                cleanDefectName(value),
            ),
        )

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
        val normalizedCode = normalizeName(code)
        val normalizedDisplay = normalizeName(displayName)
        val invalidTokens =
            setOf(
                "A",
                "-",
                "--",
                ".",
                "N/A",
                "T/A",
                "TOTAL",
                "SUBTOTAL",
                "SUB-TOTAL",
            )
        val looksLikePartNumber =
            normalizedDisplay.matches(Regex("^[A-Z0-9\\-()/.\\s]{8,}$")) &&
                normalizedDisplay.count { it.isDigit() } >= 4
        return code.isNotBlank() &&
            displayName.length >= 3 &&
            normalizedCode !in invalidTokens &&
            normalizedDisplay !in invalidTokens &&
            !looksLikePartNumber
    }
}

@Serializable
private data class LocalStatePayload(
    val inspections: List<SavedInspection> = emptyList(),
    val customDefects: List<SavedCustomDefect> = emptyList(),
)

@Serializable
private data class SavedInspection(
    val recordId: Long,
    val kind: String,
    val lineId: Long,
    val shiftId: Long,
    val partId: Long,
    val totalCheck: Int?,
    val picName: String,
    val createdAt: String,
    val defects: List<SavedDefectEntry> = emptyList(),
)

@Serializable
private data class SavedDefectEntry(
    val defectTypeId: Long,
    val quantity: Int,
    val slots: List<SavedDefectSlot> = emptyList(),
)

@Serializable
private data class SavedDefectSlot(
    val slotCode: String,
    val quantity: Int,
)

@Serializable
private data class SavedCustomDefect(
    val id: Long,
    val code: String,
    val name: String,
    val lineCode: String?,
)

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
