package id.co.nierstyd.mutugemba.usecase

import id.co.nierstyd.mutugemba.domain.InspectionDefectEntry
import id.co.nierstyd.mutugemba.domain.InspectionInput
import id.co.nierstyd.mutugemba.domain.InspectionRecord
import id.co.nierstyd.mutugemba.domain.InspectionRepository
import id.co.nierstyd.mutugemba.domain.UserRole
import id.co.nierstyd.mutugemba.domain.createdDateOrToday
import id.co.nierstyd.mutugemba.domain.normalized
import id.co.nierstyd.mutugemba.domain.resolveDefectEntries
import java.time.LocalDate
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
        val normalizedInput = input.normalized()
        val defectEntries = normalizedInput.resolveDefectEntries()
        var feedback = validateInput(normalizedInput, defectEntries)
        var record: InspectionRecord? = null
        if (feedback.type != FeedbackType.ERROR) {
            val createdAt = normalizedInput.createdAt
            val createdDate = normalizedInput.createdDateOrToday()
            val hasDuplicate =
                actorRole != UserRole.ADMIN &&
                    !allowDuplicateSameDay &&
                    repository.hasInspectionOnDate(
                        lineId = normalizedInput.lineId,
                        partId = normalizedInput.partId,
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
                    normalizedInput.copy(
                        defects = defectEntries,
                        defectTypeId = null,
                        defectQuantity = null,
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

    private fun validateInput(
        input: InspectionInput,
        defectEntries: List<InspectionDefectEntry>,
    ): UserFeedback {
        if (input.lineId <= 0L || input.shiftId <= 0L || input.partId <= 0L) {
            return UserFeedback(
                FeedbackType.ERROR,
                "Lengkapi Line, Shift, dan Part terlebih dahulu.",
            )
        }

        val createdDate = input.createdDateOrToday()
        val today = LocalDate.now()
        if (createdDate.isAfter(today) || createdDate.isBefore(today)) {
            return UserFeedback(
                FeedbackType.ERROR,
                "Input checksheet hanya diperbolehkan untuk hari ini.",
            )
        }

        val totalDefect = defectEntries.sumOf { it.totalQuantity }
        val totalCheck = input.totalCheck
        val hasTotalCheck = (totalCheck ?: 0) > 0
        val totalCheckInvalid = totalCheck != null && totalCheck < totalDefect
        return when {
            defectEntries.isEmpty() && !hasTotalCheck ->
                UserFeedback(FeedbackType.ERROR, "Isi jumlah NG terlebih dahulu.")
            defectEntries.any { it.totalQuantity <= 0 } ->
                UserFeedback(FeedbackType.ERROR, "Jumlah NG harus lebih dari 0.")
            totalCheckInvalid ->
                UserFeedback(FeedbackType.ERROR, "Total periksa harus lebih besar atau sama dengan total NG.")
            else -> UserFeedback(FeedbackType.INFO, "Validasi OK. Siap disimpan.")
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
                        FeedbackType.SUCCESS,
                        "Data inspeksi tersimpan ($saved part). Part lain dilewati.",
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
