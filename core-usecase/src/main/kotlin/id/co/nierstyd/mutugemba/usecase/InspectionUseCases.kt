package id.co.nierstyd.mutugemba.usecase

import id.co.nierstyd.mutugemba.domain.InspectionDefectEntry
import id.co.nierstyd.mutugemba.domain.InspectionInput
import id.co.nierstyd.mutugemba.domain.InspectionRecord
import id.co.nierstyd.mutugemba.domain.InspectionRepository
import id.co.nierstyd.mutugemba.domain.UserRole
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class FeedbackType {
    INFO,
    WARNING,
    ERROR,
    SUCCESS,
}

data class UserFeedback(
    val type: FeedbackType,
    val message: String,
)

data class CreateInspectionResult(
    val record: InspectionRecord?,
    val feedback: UserFeedback,
)

data class PartSaveResult(
    val partId: Long,
    val record: InspectionRecord?,
    val feedback: UserFeedback,
)

data class BatchInspectionResult(
    val results: List<PartSaveResult>,
    val feedback: UserFeedback,
) {
    val savedRecords: List<InspectionRecord>
        get() = results.mapNotNull { it.record }

    val failedParts: List<PartSaveResult>
        get() = results.filter { it.record == null }
}

class CreateInspectionRecordUseCase(
    private val repository: InspectionRepository,
) {
    fun execute(
        input: InspectionInput,
        actorRole: UserRole = UserRole.USER,
        allowDuplicateSameDay: Boolean = false,
    ): CreateInspectionResult {
        var feedback = validateInput(input)
        var record: InspectionRecord? = null
        if (feedback.type != FeedbackType.ERROR) {
            val createdAt =
                input.createdAt
                    .trim()
                    .ifBlank {
                        LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    }
            val createdDate =
                runCatching { LocalDateTime.parse(createdAt).toLocalDate() }
                    .getOrElse { LocalDate.now() }
            val hasDuplicate =
                actorRole != UserRole.ADMIN &&
                    !allowDuplicateSameDay &&
                    repository.hasInspectionOnDate(
                        lineId = input.lineId,
                        partId = input.partId,
                        date = createdDate,
                    )
            if (hasDuplicate) {
                val dateLabel =
                    createdDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("id", "ID")))
                feedback =
                    UserFeedback(
                        FeedbackType.ERROR,
                        "Data inspeksi tanggal $dateLabel sudah ada. Aturan input ulang hari yang sama sedang aktif.",
                    )
            } else {
                val cleanedInput =
                    input.copy(
                        createdAt = createdAt,
                    )
                record = repository.insert(cleanedInput)
                feedback = UserFeedback(FeedbackType.SUCCESS, "Data inspeksi tersimpan.")
            }
        }

        return CreateInspectionResult(
            record = record,
            feedback = feedback,
        )
    }

    private fun validateInput(input: InspectionInput): UserFeedback {
        if (input.lineId <= 0L || input.shiftId <= 0L || input.partId <= 0L) {
            return UserFeedback(
                FeedbackType.ERROR,
                "Lengkapi Line, Shift, dan Part terlebih dahulu.",
            )
        }

        val defectEntries = resolveDefectEntries(input)
        val totalDefect = defectEntries.sumOf { it.totalQuantity }
        val totalCheck = input.totalCheck
        val totalCheckInvalid = totalCheck != null && totalCheck < totalDefect
        return when {
            defectEntries.isEmpty() ->
                UserFeedback(FeedbackType.ERROR, "Isi jumlah NG terlebih dahulu.")
            defectEntries.any { it.totalQuantity <= 0 } ->
                UserFeedback(FeedbackType.ERROR, "Jumlah NG harus lebih dari 0.")
            totalCheckInvalid ->
                UserFeedback(FeedbackType.ERROR, "Total periksa harus lebih besar atau sama dengan total NG.")
            else -> UserFeedback(FeedbackType.INFO, "Validasi OK. Siap disimpan.")
        }
    }

    private fun resolveDefectEntries(input: InspectionInput): List<InspectionDefectEntry> {
        if (input.defects.isNotEmpty()) {
            return input.defects
        }
        val defectTypeId = input.defectTypeId
        val defectQuantity = input.defectQuantity
        return if (defectTypeId != null && defectQuantity != null && defectQuantity > 0) {
            listOf(
                InspectionDefectEntry(
                    defectTypeId = defectTypeId,
                    quantity = defectQuantity,
                ),
            )
        } else {
            emptyList()
        }
    }
}

class CreateBatchInspectionRecordsUseCase(
    private val createInspectionRecordUseCase: CreateInspectionRecordUseCase,
) {
    fun execute(
        inputs: List<InspectionInput>,
        actorRole: UserRole = UserRole.USER,
        allowDuplicateSameDay: Boolean = false,
    ): BatchInspectionResult {
        if (inputs.isEmpty()) {
            return BatchInspectionResult(
                results = emptyList(),
                feedback = UserFeedback(FeedbackType.ERROR, "Isi minimal satu part sebelum disimpan."),
            )
        }

        val results =
            inputs.map { input ->
                val result =
                    createInspectionRecordUseCase.execute(
                        input = input,
                        actorRole = actorRole,
                        allowDuplicateSameDay = allowDuplicateSameDay,
                    )
                PartSaveResult(
                    partId = input.partId,
                    record = result.record,
                    feedback = result.feedback,
                )
            }

        val failed = results.count { it.record == null }
        val saved = results.size - failed
        val feedback =
            when {
                failed == 0 ->
                    UserFeedback(FeedbackType.SUCCESS, "Semua data inspeksi tersimpan ($saved part).")
                failed == results.size ->
                    UserFeedback(FeedbackType.ERROR, "Semua data gagal disimpan. Periksa input.")
                else ->
                    UserFeedback(
                        FeedbackType.WARNING,
                        "Sebagian data tersimpan ($saved part). Ada $failed part gagal disimpan.",
                    )
            }

        return BatchInspectionResult(results = results, feedback = feedback)
    }
}

class GetRecentInspectionsUseCase(
    private val repository: InspectionRepository,
) {
    fun execute(limit: Long = 20): List<InspectionRecord> = repository.getRecent(limit)
}
