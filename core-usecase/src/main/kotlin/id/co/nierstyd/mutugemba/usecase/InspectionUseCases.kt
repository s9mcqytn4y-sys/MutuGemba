package id.co.nierstyd.mutugemba.usecase

import id.co.nierstyd.mutugemba.domain.InspectionInput
import id.co.nierstyd.mutugemba.domain.InspectionKind
import id.co.nierstyd.mutugemba.domain.InspectionRecord
import id.co.nierstyd.mutugemba.domain.InspectionRepository
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
    fun execute(input: InspectionInput): CreateInspectionResult {
        val validation = validateInput(input)
        if (validation.type == FeedbackType.ERROR) {
            return CreateInspectionResult(record = null, feedback = validation)
        }

        val createdAt = input.createdAt.ifBlank {
            LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        }
        val cleanedInput =
            input.copy(
                defectQuantity = input.defectQuantity?.coerceAtLeast(0),
                ctqValue = input.ctqValue,
                createdAt = createdAt,
            )

        val record = repository.insert(cleanedInput)
        return CreateInspectionResult(
            record = record,
            feedback = UserFeedback(FeedbackType.SUCCESS, "Data inspeksi tersimpan."),
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
                if (input.defectTypeId == null || input.defectTypeId <= 0L) {
                    UserFeedback(FeedbackType.ERROR, "Pilih jenis cacat terlebih dahulu.")
                } else if (input.defectQuantity == null || input.defectQuantity <= 0) {
                    UserFeedback(FeedbackType.ERROR, "Jumlah cacat harus lebih dari 0.")
                } else {
                    UserFeedback(FeedbackType.INFO, "Validasi OK. Siap disimpan.")
                }
            }

            InspectionKind.CTQ -> {
                if (input.ctqParameterId == null || input.ctqParameterId <= 0L) {
                    UserFeedback(FeedbackType.ERROR, "Pilih parameter CTQ terlebih dahulu.")
                } else if (input.ctqValue == null) {
                    UserFeedback(FeedbackType.ERROR, "Isi nilai CTQ terlebih dahulu.")
                } else {
                    UserFeedback(FeedbackType.INFO, "Validasi OK. Siap disimpan.")
                }
            }
        }
    }
}

class GetRecentInspectionsUseCase(
    private val repository: InspectionRepository,
) {
    fun execute(limit: Long = 20): List<InspectionRecord> = repository.getRecent(limit)
}
