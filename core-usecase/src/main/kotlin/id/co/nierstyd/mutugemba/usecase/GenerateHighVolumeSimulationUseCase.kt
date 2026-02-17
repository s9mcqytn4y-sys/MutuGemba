package id.co.nierstyd.mutugemba.usecase

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

class GenerateHighVolumeSimulationUseCase(
    private val inspectionRepository: InspectionRepository,
    private val masterDataRepository: MasterDataRepository,
) {
    fun execute(
        days: Int = 45,
        density: Int = 4,
        seed: Long? = null,
    ): Int {
        val lines = masterDataRepository.getLines()
        val shifts = masterDataRepository.getShifts()
        val parts = masterDataRepository.getParts()
        val defectTypes = masterDataRepository.getDefectTypes()
        val hasMissingMasterData = listOf(lines, shifts, parts, defectTypes).any { it.isEmpty() }
        if (hasMissingMasterData) return 0

        val now = LocalDate.now()
        val random = seed?.let { Random(it) } ?: Random(System.currentTimeMillis())
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
        var inserted = 0
        val fallbackDefectCodesByMaterial = buildFallbackDefectCodesByMaterial(parts)
        val defectByCode = defectTypes.associateBy { it.code }

        repeat(days.coerceAtLeast(1)) { dayOffset ->
            val date = now.minusDays(dayOffset.toLong())
            val isWeekend = date.dayOfWeek.value >= 6
            lines.forEach { line ->
                val lineParts = parts.filter { it.lineCode == line.code }
                if (lineParts.isEmpty()) return@forEach

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
                    repeat(dailyDensity) { densityIndex ->
                        val candidateDefects =
                            resolveCandidateDefects(
                                part = part,
                                lineCode = line.code,
                                defectByCode = defectByCode,
                                fallbackDefectCodesByMaterial = fallbackDefectCodesByMaterial,
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
                        val clock = LocalTime.of((8 + (slotIndex % 8)).coerceAtMost(16), random.nextInt(0, 59))
                        val createdAt = LocalDateTime.of(date, clock).plusMinutes((densityIndex * 7).toLong())
                        inspectionRepository.insert(
                            InspectionInput(
                                kind = InspectionKind.DEFECT,
                                lineId = line.id,
                                shiftId = shiftId,
                                partId = part.id,
                                totalCheck = totalCheck,
                                defectTypeId = null,
                                defectQuantity = null,
                                defects = entries,
                                picName = "Simulasi-${line.name}",
                                createdAt = createdAt.format(formatter),
                            ),
                        )
                        inserted += 1
                    }
                }
            }
        }

        return inserted
    }

    private fun resolveCandidateDefects(
        part: Part,
        lineCode: LineCode,
        defectByCode: Map<String, DefectType>,
        fallbackDefectCodesByMaterial: Map<String, List<String>>,
    ): List<DefectType> {
        val recommended =
            part.recommendedDefectCodes
                .mapNotNull { code -> defectByCode[code] }
                .filter { defect -> defect.lineCode == null || defect.lineCode == lineCode }
                .distinctBy { it.id }
        val materialKey = part.material.trim().uppercase()
        val inferredByMaterial =
            fallbackDefectCodesByMaterial[materialKey]
                .orEmpty()
                .mapNotNull { code -> defectByCode[code] }
                .filter { defect -> defect.lineCode == null || defect.lineCode == lineCode }
                .distinctBy { it.id }
        return when {
            recommended.isNotEmpty() -> recommended
            inferredByMaterial.isNotEmpty() -> inferredByMaterial
            else -> emptyList()
        }
    }

    private fun buildFallbackDefectCodesByMaterial(parts: List<Part>): Map<String, List<String>> =
        parts
            .groupBy { it.material.trim().uppercase() }
            .mapValues { (_, groupedParts) ->
                groupedParts
                    .flatMap { it.recommendedDefectCodes }
                    .groupingBy { it }
                    .eachCount()
                    .entries
                    .sortedByDescending { it.value }
                    .map { it.key }
            }
}
