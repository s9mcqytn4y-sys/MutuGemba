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

    @Suppress("LongMethod")
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

        repeat(safeDays) { dayOffset ->
            val date = now.minusDays(dayOffset.toLong())
            val isWeekend = date.dayOfWeek.value >= 6
            lines.forEach { line ->
                val lineParts = parts.filter { it.lineCode == line.code }
                if (lineParts.isEmpty()) return@forEach
                var insertedForLineOnDate = 0

                val minBatchSize = minOf(8, lineParts.size)
                val loadMultiplier = if (isWeekend) 0.55 else 1.0
                val randomizedBatchRatio = (0.28 + random.nextDouble() * 0.36) * loadMultiplier
                val batchSize = (lineParts.size * randomizedBatchRatio).toInt().coerceIn(minBatchSize, lineParts.size)
                val shiftId = shifts[random.nextInt(shifts.size)].id
                lineParts.shuffled(random).take(batchSize).forEachIndexed { slotIndex, part ->
                    val weekendMultiplier = if (isWeekend) 0.7 else 1.0
                    val dailyDensity =
                        (density.coerceAtLeast(1).toDouble() * weekendMultiplier)
                            .toInt()
                            .coerceAtLeast(1)
                    val lineInserted =
                        insertPartSimulation(
                            part = part,
                            lineCode = line.code,
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
                            resolverContext = resolverContext,
                        )
                    inserted += lineInserted
                    insertedForLineOnDate += lineInserted
                    insertedByLineId[line.id] = (insertedByLineId[line.id] ?: 0) + lineInserted
                }

                if (insertedForLineOnDate == 0) {
                    val fallbackPart =
                        lineParts.firstOrNull { part ->
                            resolveCandidateDefects(
                                part = part,
                                lineCode = line.code,
                                defectByCode = resolverContext.byCode,
                                defectByCanonicalName = resolverContext.byCanonicalName,
                                fallbackDefectCodesByMaterial = resolverContext.fallbackCodesByMaterial,
                                fallbackDefectCodesByLine = resolverContext.fallbackCodesByLine,
                            ).isNotEmpty()
                        }
                    if (fallbackPart != null) {
                        val fallbackInserted =
                            insertPartSimulation(
                                part = fallbackPart,
                                lineCode = line.code,
                                batchContext =
                                    SimulationBatchContext(
                                        lineId = line.id,
                                        lineName = line.name,
                                        shiftId = shiftId,
                                        date = date,
                                        slotIndex = 0,
                                        density = 1,
                                        formatter = formatter,
                                    ),
                                random = random,
                                resolverContext = resolverContext,
                            )
                        inserted += fallbackInserted
                        insertedByLineId[line.id] = (insertedByLineId[line.id] ?: 0) + fallbackInserted
                    }
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
        lineCode: LineCode,
        batchContext: SimulationBatchContext,
        random: Random,
        resolverContext: DefectResolverContext,
    ): Int {
        var inserted = 0
        repeat(batchContext.density) { densityIndex ->
            val candidateDefects =
                resolveCandidateDefects(
                    part = part,
                    lineCode = lineCode,
                    defectByCode = resolverContext.byCode,
                    defectByCanonicalName = resolverContext.byCanonicalName,
                    fallbackDefectCodesByMaterial = resolverContext.fallbackCodesByMaterial,
                    fallbackDefectCodesByLine = resolverContext.fallbackCodesByLine,
                )
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
}
