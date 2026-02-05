package id.co.nierstyd.mutugemba.usecase

import id.co.nierstyd.mutugemba.domain.InspectionDefectEntry
import id.co.nierstyd.mutugemba.domain.InspectionInput
import id.co.nierstyd.mutugemba.domain.InspectionKind
import id.co.nierstyd.mutugemba.domain.InspectionRecord
import id.co.nierstyd.mutugemba.domain.InspectionRepository
import id.co.nierstyd.mutugemba.domain.UserRole
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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

class CreateInspectionRecordUseCase(
    private val repository: InspectionRepository,
) {
    fun execute(
        input: InspectionInput,
        actorRole: UserRole = UserRole.USER,
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
                    repository.hasInspectionOnDate(
                        lineId = input.lineId,
                        partId = input.partId,
                        date = createdDate,
                    )
            if (hasDuplicate) {
                feedback = UserFeedback(FeedbackType.ERROR, "Data inspeksi hari ini sudah ada.")
            } else {
                val cleanedInput =
                    input.copy(
                        ctqValue = input.ctqValue,
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

        return when (input.kind) {
            InspectionKind.DEFECT -> {
                val defectEntries = resolveDefectEntries(input)
                when {
                    defectEntries.isEmpty() ->
                        UserFeedback(FeedbackType.ERROR, "Isi jumlah cacat terlebih dahulu.")
                    defectEntries.any { it.totalQuantity <= 0 } ->
                        UserFeedback(FeedbackType.ERROR, "Jumlah cacat harus lebih dari 0.")
                    else -> UserFeedback(FeedbackType.INFO, "Validasi OK. Siap disimpan.")
                }
            }

            InspectionKind.CTQ -> {
                val ctqParameterId = input.ctqParameterId
                val ctqValue = input.ctqValue
                if (ctqParameterId == null || ctqParameterId <= 0L) {
                    UserFeedback(FeedbackType.ERROR, "Pilih parameter CTQ terlebih dahulu.")
                } else if (ctqValue == null || ctqValue.isNaN()) {
                    UserFeedback(FeedbackType.ERROR, "Isi nilai CTQ terlebih dahulu.")
                } else {
                    UserFeedback(FeedbackType.INFO, "Validasi OK. Siap disimpan.")
                }
            }
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

class GetRecentInspectionsUseCase(
    private val repository: InspectionRepository,
) {
    fun execute(limit: Long = 20): List<InspectionRecord> = repository.getRecent(limit)
}
