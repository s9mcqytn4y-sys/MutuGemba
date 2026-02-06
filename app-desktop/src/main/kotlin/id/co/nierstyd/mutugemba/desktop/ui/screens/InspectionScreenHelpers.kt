package id.co.nierstyd.mutugemba.desktop.ui.screens

import id.co.nierstyd.mutugemba.desktop.ui.components.MilestoneStatus
import id.co.nierstyd.mutugemba.domain.Shift

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
