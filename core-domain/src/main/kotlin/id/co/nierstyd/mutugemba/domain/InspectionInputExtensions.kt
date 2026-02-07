package id.co.nierstyd.mutugemba.domain

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object InspectionInputDefaults {
    const val DEFAULT_PIC_NAME = "Admin QC"
}

fun InspectionInput.normalized(now: LocalDateTime = LocalDateTime.now()): InspectionInput {
    val normalizedCreatedAt =
        createdAt
            .trim()
            .ifBlank { now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) }
    val normalizedPicName =
        picName
            .trim()
            .ifBlank { InspectionInputDefaults.DEFAULT_PIC_NAME }
    return copy(
        createdAt = normalizedCreatedAt,
        picName = normalizedPicName,
    )
}

fun InspectionInput.createdDateOrToday(today: LocalDate = LocalDate.now()): LocalDate =
    runCatching { LocalDateTime.parse(createdAt).toLocalDate() }
        .getOrElse { today }

fun InspectionInput.resolveDefectEntries(): List<InspectionDefectEntry> {
    if (defects.isNotEmpty()) {
        return defects
    }
    val resolvedDefectTypeId = defectTypeId
    val resolvedDefectQuantity = defectQuantity
    return if (resolvedDefectTypeId != null && resolvedDefectQuantity != null && resolvedDefectQuantity > 0) {
        listOf(
            InspectionDefectEntry(
                defectTypeId = resolvedDefectTypeId,
                quantity = resolvedDefectQuantity,
            ),
        )
    } else {
        emptyList()
    }
}
