package id.co.nierstyd.mutugemba.desktop.ui.screens

import id.co.nierstyd.mutugemba.desktop.ui.components.MilestoneStatus
import id.co.nierstyd.mutugemba.domain.CtqParameter
import id.co.nierstyd.mutugemba.domain.Shift
import id.co.nierstyd.mutugemba.usecase.FeedbackType
import id.co.nierstyd.mutugemba.usecase.UserFeedback
import java.util.Locale

internal fun resolveSelection(
    currentId: Long?,
    preferredId: Long?,
    options: List<Long>,
): Long? =
    listOfNotNull(currentId, preferredId)
        .firstOrNull { options.contains(it) }
        ?: options.firstOrNull()

internal fun milestoneStatus(
    currentStep: Int,
    stepIndex: Int,
    isComplete: Boolean,
): MilestoneStatus =
    when {
        isComplete -> MilestoneStatus.DONE
        currentStep == stepIndex -> MilestoneStatus.ACTIVE
        else -> MilestoneStatus.PENDING
    }

internal fun shiftTimeLabel(shift: Shift): String = "${shift.startTime ?: "-"} - ${shift.endTime ?: "-"}"

internal fun ctqLimitLabel(parameter: CtqParameter): String {
    val lower = formatDecimal(parameter.lowerLimit)
    val upper = formatDecimal(parameter.upperLimit)
    val target = formatDecimal(parameter.targetValue)
    return "Target $target ${parameter.unit} (LL $lower, UL $upper)"
}

internal fun formatDecimal(value: Double?): String = value?.let { String.format(Locale.US, "%.2f", it) } ?: "-"

internal fun buildCtqWarning(
    parameter: CtqParameter?,
    value: Double?,
): UserFeedback? {
    if (parameter == null || value == null) {
        return null
    }
    val lower = parameter.lowerLimit
    val upper = parameter.upperLimit
    val outOfRange = (lower != null && value < lower) || (upper != null && value > upper)
    return if (outOfRange) {
        UserFeedback(
            FeedbackType.WARNING,
            "Nilai CTQ di luar batas standar. Pastikan pengecekan ulang.",
        )
    } else {
        null
    }
}
