package id.co.nierstyd.mutugemba.usecase

import id.co.nierstyd.mutugemba.domain.InspectionDefectEntry
import id.co.nierstyd.mutugemba.domain.InspectionInput
import id.co.nierstyd.mutugemba.domain.InspectionKind
import id.co.nierstyd.mutugemba.domain.InspectionRepository
import id.co.nierstyd.mutugemba.domain.MasterDataRepository
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
    ): Int {
        val lines = masterDataRepository.getLines()
        val shifts = masterDataRepository.getShifts()
        val parts = masterDataRepository.getParts()
        val defectTypes = masterDataRepository.getDefectTypes()
        val hasMissingMasterData = listOf(lines, shifts, parts, defectTypes).any { it.isEmpty() }
        if (hasMissingMasterData) return 0

        val shiftId = shifts.first().id
        val now = LocalDate.now()
        val random = Random(240211L)
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
        var inserted = 0

        repeat(days.coerceAtLeast(1)) { dayOffset ->
            val date = now.minusDays(dayOffset.toLong())
            lines.forEach { line ->
                val lineParts = parts.filter { it.lineCode == line.code }
                if (lineParts.isEmpty()) return@forEach

                val minBatchSize = minOf(8, lineParts.size)
                val batchSize = (lineParts.size * 0.4).toInt().coerceIn(minBatchSize, lineParts.size)
                lineParts.shuffled(random).take(batchSize).forEachIndexed { slotIndex, part ->
                    repeat(density.coerceAtLeast(1)) { densityIndex ->
                        val candidateDefects =
                            part.recommendedDefectCodes
                                .mapNotNull { code -> defectTypes.firstOrNull { it.code == code } }
                                .ifEmpty {
                                    defectTypes
                                        .filter { it.lineCode == null || it.lineCode == line.code }
                                        .shuffled(random)
                                        .take(3)
                                }
                        if (candidateDefects.isEmpty()) return@repeat

                        val picked =
                            candidateDefects
                                .shuffled(random)
                                .take((1 + random.nextInt(2)).coerceAtMost(candidateDefects.size))
                        val entries =
                            picked.map { defect ->
                                val q1 = random.nextInt(0, 4)
                                val q2 = random.nextInt(0, 4)
                                val q3 = random.nextInt(0, 3)
                                val total = q1 + q2 + q3
                                InspectionDefectEntry(
                                    defectTypeId = defect.id,
                                    quantity = total.coerceAtLeast(1),
                                    slots = emptyList(),
                                )
                            }
                        val totalDefect = entries.sumOf { it.quantity }.coerceAtLeast(1)
                        val totalCheck = totalDefect + random.nextInt(8, 40)
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
}
