package id.co.nierstyd.mutugemba.usecase

import id.co.nierstyd.mutugemba.domain.DefectNameSanitizer
import id.co.nierstyd.mutugemba.domain.DefectType
import id.co.nierstyd.mutugemba.domain.InspectionDefectEntry
import id.co.nierstyd.mutugemba.domain.InspectionInput
import id.co.nierstyd.mutugemba.domain.InspectionKind
import id.co.nierstyd.mutugemba.domain.InspectionRepository
import id.co.nierstyd.mutugemba.domain.LineCode
import id.co.nierstyd.mutugemba.domain.MasterDataRepository
import id.co.nierstyd.mutugemba.domain.Part
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.random.Random

data class SimulationLineSummary(
    val lineName: String,
    val insertedRecords: Int,
)

data class HighVolumeSimulationSummary(
    val insertedRecords: Int,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val lineBreakdown: List<SimulationLineSummary>,
)

class GenerateHighVolumeSimulationUseCase(
    private val inspectionRepository: InspectionRepository,
    private val masterDataRepository: MasterDataRepository,
) {
    private data class DefectResolverContext(
        val byCode: Map<String, DefectType>,
        val byCanonicalName: Map<String, DefectType>,
        val fallbackCodesByMaterial: Map<String, List<String>>,
        val fallbackCodesByLine: Map<LineCode, List<String>>,
    )

    private data class SimulationBatchContext(
        val lineId: Long,
        val lineName: String,
        val shiftId: Long,
        val date: LocalDate,
        val slotIndex: Int,
        val density: Int,
        val formatter: DateTimeFormatter,
    )

    fun execute(
        days: Int = 45,
        density: Int = 4,
        seed: Long? = null,
    ): Int =
        executeWithSummary(
            days = days,
            density = density,
            seed = seed,
        ).insertedRecords

    @Suppress("LongMethod", "CyclomaticComplexMethod")
    fun executeWithSummary(
        days: Int = 45,
        density: Int = 4,
        seed: Long? = null,
    ): HighVolumeSimulationSummary {
        val lines = masterDataRepository.getLines()
        val shifts = masterDataRepository.getShifts()
        val parts = masterDataRepository.getParts()
        val defectTypes = masterDataRepository.getDefectTypes()
        val hasMissingMasterData = listOf(lines, shifts, parts, defectTypes).any { it.isEmpty() }
        val safeDays = days.coerceAtLeast(1)
        val now = LocalDate.now()
        val startDate = now.minusDays((safeDays - 1).toLong())
        if (hasMissingMasterData) {
            return HighVolumeSimulationSummary(
                insertedRecords = 0,
                startDate = startDate,
                endDate = now,
                lineBreakdown = emptyList(),
            )
        }

        val random = seed?.let { Random(it) } ?: Random(System.currentTimeMillis())
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
        var inserted = 0
        val insertedByLineId = mutableMapOf<Long, Int>()
        val resolverContext =
            DefectResolverContext(
                byCode = defectTypes.associateBy { it.code.trim().uppercase() },
                byCanonicalName = defectTypes.associateBy { DefectNameSanitizer.canonicalKey(it.name) },
                fallbackCodesByMaterial = buildFallbackDefectCodesByMaterial(parts),
                fallbackCodesByLine = buildFallbackDefectCodesByLine(parts),
            )
        val candidateDefectsByPartId =
            parts.associate { part ->
                part.id to
                    resolveCandidateDefects(
                        part = part,
                        lineCode = part.lineCode,
                        defectByCode = resolverContext.byCode,
                        defectByCanonicalName = resolverContext.byCanonicalName,
                        fallbackDefectCodesByMaterial = resolverContext.fallbackCodesByMaterial,
                        fallbackDefectCodesByLine = resolverContext.fallbackCodesByLine,
                    )
            }
        val safeDensity = density.coerceAtLeast(1)

        repeat(safeDays) { dayOffset ->
            val date = now.minusDays(dayOffset.toLong())
            val isWeekend = date.dayOfWeek.value >= 6
            lines.forEach { line ->
                val lineParts = parts.filter { it.lineCode == line.code }
                if (lineParts.isEmpty()) return@forEach
                val eligibleParts =
                    lineParts.filter { part ->
                        candidateDefectsByPartId[part.id].orEmpty().isNotEmpty()
                    }
                if (eligibleParts.isEmpty()) return@forEach

                val coverageBaseRatio = if (isWeekend) 0.5 else 0.75
                val coverageRatio = (coverageBaseRatio + random.nextDouble(-0.12, 0.18)).coerceIn(0.35, 1.0)
                val minBatchSize = minOf(maxOf(2, safeDensity), eligibleParts.size)
                val baseBatchSize =
                    (eligibleParts.size * coverageRatio)
                        .toInt()
                        .coerceIn(minBatchSize, eligibleParts.size)
                val rotationSeed = dayOffset + line.id.toInt() + safeDensity
                val rotatedParts = eligibleParts.rotateFromIndex(rotationSeed)
                val extraSpotChecks = if (isWeekend) 0 else (safeDensity / 2).coerceAtMost(2)
                val fallbackPartIds =
                    eligibleParts
                        .filter { it.recommendedDefectCodes.isEmpty() }
                        .map { it.id }
                        .toSet()
                var selectedParts =
                    (rotatedParts.take(baseBatchSize) + rotatedParts.shuffled(random).take(extraSpotChecks))
                        .distinctBy { it.id }
                if (fallbackPartIds.isNotEmpty() && selectedParts.none { it.id in fallbackPartIds }) {
                    val fallbackPart = rotatedParts.firstOrNull { it.id in fallbackPartIds }
                    if (fallbackPart != null) {
                        selectedParts = (selectedParts + fallbackPart).distinctBy { it.id }
                    }
                }
                val shiftId = shifts[random.nextInt(shifts.size)].id
                selectedParts.forEachIndexed { slotIndex, part ->
                    val weekendPenalty = if (isWeekend) 1 else 0
                    val densityVariance = if (safeDensity <= 1) 0 else random.nextInt(0, 2)
                    val dailyDensity = (safeDensity - weekendPenalty + densityVariance).coerceAtLeast(1)
                    val candidateDefects = candidateDefectsByPartId[part.id].orEmpty()
                    val lineInserted =
                        insertPartSimulation(
                            part = part,
                            candidateDefects = candidateDefects,
                            batchContext =
                                SimulationBatchContext(
                                    lineId = line.id,
                                    lineName = line.name,
                                    shiftId = shiftId,
                                    date = date,
                                    slotIndex = slotIndex,
                                    density = dailyDensity,
                                    formatter = formatter,
                                ),
                            random = random,
                        )
                    inserted += lineInserted
                    insertedByLineId[line.id] = (insertedByLineId[line.id] ?: 0) + lineInserted
                }
            }
        }

        val lineBreakdown =
            lines
                .mapNotNull { line ->
                    val count = insertedByLineId[line.id] ?: return@mapNotNull null
                    if (count <= 0) return@mapNotNull null
                    SimulationLineSummary(
                        lineName = line.name,
                        insertedRecords = count,
                    )
                }.sortedByDescending { it.insertedRecords }

        return HighVolumeSimulationSummary(
            insertedRecords = inserted,
            startDate = startDate,
            endDate = now,
            lineBreakdown = lineBreakdown,
        )
    }

    private fun resolveCandidateDefects(
        part: Part,
        lineCode: LineCode,
        defectByCode: Map<String, DefectType>,
        defectByCanonicalName: Map<String, DefectType>,
        fallbackDefectCodesByMaterial: Map<String, List<String>>,
        fallbackDefectCodesByLine: Map<LineCode, List<String>>,
    ): List<DefectType> {
        val recommended =
            part.recommendedDefectCodes
                .mapNotNull { raw ->
                    defectByCode[raw.trim().uppercase()]
                        ?: defectByCanonicalName[DefectNameSanitizer.canonicalKey(raw)]
                }.filter { defect -> defect.lineCode == null || defect.lineCode == lineCode }
                .distinctBy { it.id }
        val materialKey = part.material.trim().uppercase()
        val inferredByMaterial =
            fallbackDefectCodesByMaterial[materialKey]
                .orEmpty()
                .mapNotNull { code ->
                    defectByCode[code.trim().uppercase()]
                        ?: defectByCanonicalName[DefectNameSanitizer.canonicalKey(code)]
                }.filter { defect -> defect.lineCode == null || defect.lineCode == lineCode }
                .distinctBy { it.id }
        val inferredByLine =
            fallbackDefectCodesByLine[lineCode]
                .orEmpty()
                .mapNotNull { code ->
                    defectByCode[code.trim().uppercase()]
                        ?: defectByCanonicalName[DefectNameSanitizer.canonicalKey(code)]
                }.filter { defect -> defect.lineCode == null || defect.lineCode == lineCode }
                .distinctBy { it.id }
        return when {
            recommended.isNotEmpty() -> recommended
            inferredByMaterial.isNotEmpty() -> inferredByMaterial
            inferredByLine.isNotEmpty() -> inferredByLine
            else -> emptyList()
        }
    }

    private fun buildFallbackDefectCodesByMaterial(parts: List<Part>): Map<String, List<String>> =
        parts
            .groupBy { it.material.trim().uppercase() }
            .mapValues { (_, groupedParts) ->
                groupedParts
                    .flatMap { it.recommendedDefectCodes }
                    .map { it.trim().uppercase() }
                    .filter { it.isNotBlank() }
                    .groupingBy { it }
                    .eachCount()
                    .entries
                    .sortedByDescending { it.value }
                    .map { it.key }
            }

    private fun buildFallbackDefectCodesByLine(parts: List<Part>): Map<LineCode, List<String>> =
        parts
            .groupBy { it.lineCode }
            .mapValues { (_, groupedParts) ->
                groupedParts
                    .flatMap { it.recommendedDefectCodes }
                    .map { it.trim().uppercase() }
                    .filter { it.isNotBlank() }
                    .groupingBy { it }
                    .eachCount()
                    .entries
                    .sortedByDescending { it.value }
                    .map { it.key }
            }

    private fun insertPartSimulation(
        part: Part,
        candidateDefects: List<DefectType>,
        batchContext: SimulationBatchContext,
        random: Random,
    ): Int {
        var inserted = 0
        repeat(batchContext.density) { densityIndex ->
            if (candidateDefects.isEmpty()) return@repeat

            val picked =
                candidateDefects
                    .shuffled(random)
                    .take((1 + random.nextInt(2)).coerceAtMost(candidateDefects.size))
            val entries =
                picked.map { defect ->
                    val base = random.nextInt(0, 4)
                    val extra = if (random.nextInt(100) < 18) random.nextInt(2, 7) else 0
                    val total = base + extra
                    InspectionDefectEntry(
                        defectTypeId = defect.id,
                        quantity = total.coerceAtLeast(1),
                        slots = emptyList(),
                    )
                }
            val totalDefect = entries.sumOf { it.quantity }.coerceAtLeast(1)
            val targetRatio = random.nextDouble(0.015, 0.16)
            val expectedCheckFromRatio = (totalDefect.toDouble() / targetRatio).toInt()
            val totalCheck =
                maxOf(
                    totalDefect + random.nextInt(15, 70),
                    expectedCheckFromRatio,
                )
            val clock = LocalTime.of((8 + (batchContext.slotIndex % 8)).coerceAtMost(16), random.nextInt(0, 59))
            val createdAt = LocalDateTime.of(batchContext.date, clock).plusMinutes((densityIndex * 7).toLong())
            inspectionRepository.insert(
                InspectionInput(
                    kind = InspectionKind.DEFECT,
                    lineId = batchContext.lineId,
                    shiftId = batchContext.shiftId,
                    partId = part.id,
                    totalCheck = totalCheck,
                    defectTypeId = null,
                    defectQuantity = null,
                    defects = entries,
                    picName = "Simulasi-${batchContext.lineName}",
                    createdAt = createdAt.format(batchContext.formatter),
                ),
            )
            inserted += 1
        }
        return inserted
    }

    private fun <T> List<T>.rotateFromIndex(seed: Int): List<T> {
        val safeStart = if (isEmpty()) 0 else ((seed % size) + size) % size
        return if (safeStart == 0) this else drop(safeStart) + take(safeStart)
    }
}
