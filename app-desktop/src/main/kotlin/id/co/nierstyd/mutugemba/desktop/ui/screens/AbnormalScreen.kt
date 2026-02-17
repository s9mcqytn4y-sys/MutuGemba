@file:Suppress("TooManyFunctions")

package id.co.nierstyd.mutugemba.desktop.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.co.nierstyd.mutugemba.desktop.ui.components.AppBadge
import id.co.nierstyd.mutugemba.desktop.ui.components.AppScreenContainer
import id.co.nierstyd.mutugemba.desktop.ui.components.ConfirmDialog
import id.co.nierstyd.mutugemba.desktop.ui.components.PrimaryButton
import id.co.nierstyd.mutugemba.desktop.ui.components.SecondaryButton
import id.co.nierstyd.mutugemba.desktop.ui.components.SectionHeader
import id.co.nierstyd.mutugemba.desktop.ui.components.StatusBanner
import id.co.nierstyd.mutugemba.desktop.ui.resources.AppIcons
import id.co.nierstyd.mutugemba.desktop.ui.resources.AppStrings
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralBorder
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralLight
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralSurface
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralText
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralTextMuted
import id.co.nierstyd.mutugemba.desktop.ui.theme.Spacing
import id.co.nierstyd.mutugemba.desktop.ui.theme.StatusError
import id.co.nierstyd.mutugemba.desktop.ui.theme.StatusInfo
import id.co.nierstyd.mutugemba.desktop.ui.theme.StatusSuccess
import id.co.nierstyd.mutugemba.desktop.ui.theme.StatusWarning
import id.co.nierstyd.mutugemba.usecase.FeedbackType
import id.co.nierstyd.mutugemba.usecase.UserFeedback
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private enum class JidokaStep {
    DETECT,
    CONTAIN,
    CLOSE,
}

private data class AbnormalTicketUi(
    val id: String,
    val title: String,
    val lineName: String,
    val problemSummary: String,
    val createdAt: String,
    val updatedAt: String,
    val step: JidokaStep,
)

@Composable
@Suppress("LongMethod")
fun AbnormalScreen() {
    var tickets by remember { mutableStateOf<List<AbnormalTicketUi>>(emptyList()) }
    var selectedTicketId by remember { mutableStateOf<String?>(null) }
    var showCreateConfirm by remember { mutableStateOf(false) }
    var showAdvanceConfirm by remember { mutableStateOf<String?>(null) }
    var feedback by remember { mutableStateOf<UserFeedback?>(null) }

    val openTickets = tickets.count { it.step != JidokaStep.CLOSE }
    val closedTickets = tickets.size - openTickets

    AppScreenContainer {
        SectionHeader(
            title = AppStrings.Abnormal.Title,
            subtitle = AppStrings.Abnormal.Subtitle,
        )
        StatusBanner(
            feedback = UserFeedback(FeedbackType.INFO, AppStrings.Abnormal.Guidance),
            dense = true,
        )
        feedback?.let {
            StatusBanner(
                feedback = it,
                dense = true,
                onDismiss = { feedback = null },
            )
        }

        AbnormalSummaryCard(
            totalTicket = tickets.size,
            openTicket = openTickets,
            closedTicket = closedTickets,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            PrimaryButton(
                text = AppStrings.Abnormal.CreateDummy,
                modifier = Modifier.weight(1f),
                onClick = { showCreateConfirm = true },
            )
            SecondaryButton(
                text = AppStrings.Abnormal.ResetLocal,
                modifier = Modifier.weight(1f),
                onClick = {
                    tickets = emptyList()
                    selectedTicketId = null
                    feedback = UserFeedback(FeedbackType.INFO, AppStrings.Abnormal.ResetFeedback)
                },
            )
        }

        if (tickets.isEmpty()) {
            EmptyAbnormalState(onCreate = { showCreateConfirm = true })
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                tickets.forEach { ticket ->
                    val expanded = ticket.id == selectedTicketId
                    AbnormalTicketCard(
                        ticket = ticket,
                        expanded = expanded,
                        onToggle = {
                            selectedTicketId = if (expanded) null else ticket.id
                        },
                        onAdvance = { showAdvanceConfirm = ticket.id },
                    )
                }
            }
        }
    }

    ConfirmDialog(
        open = showCreateConfirm,
        title = AppStrings.Abnormal.DialogTitle,
        message = AppStrings.Abnormal.DialogMessage,
        confirmText = AppStrings.Actions.Create,
        dismissText = AppStrings.Actions.Cancel,
        onConfirm = {
            showCreateConfirm = false
            val now = LocalDateTime.now()
            val ticket = createDummyTicket(now = now, index = tickets.size + 1)
            tickets = listOf(ticket) + tickets
            selectedTicketId = ticket.id
            feedback = UserFeedback(FeedbackType.SUCCESS, AppStrings.Abnormal.CreatedFeedback)
        },
        onDismiss = { showCreateConfirm = false },
    )

    ConfirmDialog(
        open = showAdvanceConfirm != null,
        title = AppStrings.Abnormal.AdvanceDialogTitle,
        message = AppStrings.Abnormal.AdvanceDialogMessage,
        confirmText = AppStrings.Actions.Continue,
        dismissText = AppStrings.Actions.Cancel,
        onConfirm = {
            val ticketId = showAdvanceConfirm
            if (ticketId != null) {
                val nowLabel = formatTicketTimestamp(LocalDateTime.now())
                tickets =
                    tickets.map { ticket ->
                        if (ticket.id != ticketId) {
                            ticket
                        } else {
                            ticket.copy(
                                step = ticket.step.next(),
                                updatedAt = nowLabel,
                            )
                        }
                    }
                val updated = tickets.firstOrNull { it.id == ticketId }
                feedback =
                    if (updated?.step == JidokaStep.CLOSE) {
                        UserFeedback(FeedbackType.SUCCESS, AppStrings.Abnormal.ClosedFeedback)
                    } else {
                        UserFeedback(FeedbackType.WARNING, AppStrings.Abnormal.ContainFeedback)
                    }
            }
            showAdvanceConfirm = null
        },
        onDismiss = { showAdvanceConfirm = null },
    )
}

@Composable
private fun AbnormalSummaryCard(
    totalTicket: Int,
    openTicket: Int,
    closedTicket: Int,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = NeutralSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, NeutralBorder),
        shape = MaterialTheme.shapes.medium,
        elevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            SummaryStatItem(title = AppStrings.Abnormal.TotalTicket, value = totalTicket.toString())
            SummaryStatItem(title = AppStrings.Abnormal.OpenTicket, value = openTicket.toString())
            SummaryStatItem(title = AppStrings.Abnormal.ClosedTicket, value = closedTicket.toString())
        }
    }
}

@Composable
private fun RowScope.SummaryStatItem(
    title: String,
    value: String,
) {
    Surface(
        modifier = Modifier.weight(1f),
        color = NeutralLight,
        shape = MaterialTheme.shapes.small,
        border = androidx.compose.foundation.BorderStroke(1.dp, NeutralBorder),
        elevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.caption,
                color = NeutralTextMuted,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.h6,
                color = NeutralText,
            )
        }
    }
}

@Composable
private fun EmptyAbnormalState(onCreate: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = NeutralSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, NeutralBorder),
        shape = MaterialTheme.shapes.medium,
        elevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Icon(
                imageVector = AppIcons.Abnormal,
                contentDescription = null,
                tint = StatusInfo,
                modifier = Modifier.size(22.dp),
            )
            Text(
                text = AppStrings.Abnormal.EmptyTitle,
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = AppStrings.Abnormal.EmptySubtitle,
                style = MaterialTheme.typography.body2,
                color = NeutralTextMuted,
            )
            PrimaryButton(
                text = AppStrings.Abnormal.CreateDummy,
                onClick = onCreate,
            )
        }
    }
}

@Composable
private fun AbnormalTicketCard(
    ticket: AbnormalTicketUi,
    expanded: Boolean,
    onToggle: () -> Unit,
    onAdvance: () -> Unit,
) {
    val progress by animateFloatAsState(targetValue = ticket.step.progressValue(), label = "jidoka_progress")
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onToggle() },
        color = NeutralSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, NeutralBorder),
        shape = MaterialTheme.shapes.medium,
        elevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Text(
                        text = ticket.title,
                        style = MaterialTheme.typography.subtitle1,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = AppStrings.Abnormal.ticketMeta(ticket.lineName, ticket.createdAt),
                        style = MaterialTheme.typography.caption,
                        color = NeutralTextMuted,
                    )
                }
                AppBadge(
                    text = ticket.step.badgeLabel(),
                    backgroundColor = ticket.step.badgeColor(),
                    contentColor = NeutralSurface,
                )
            }

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .background(NeutralBorder, MaterialTheme.shapes.small),
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth(progress)
                            .height(6.dp)
                            .background(ticket.step.badgeColor(), MaterialTheme.shapes.small),
                )
            }
            JidokaStepper(step = ticket.step)

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -it / 4 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { -it / 4 }),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    TicketDetailRow(
                        icon = AppIcons.Assignment,
                        label = AppStrings.Abnormal.ProblemLabel,
                        value = ticket.problemSummary,
                    )
                    TicketDetailRow(
                        icon = AppIcons.CalendarToday,
                        label = AppStrings.Abnormal.UpdatedLabel,
                        value = ticket.updatedAt,
                    )
                    if (ticket.step != JidokaStep.CLOSE) {
                        SecondaryButton(
                            text = ticket.step.nextActionLabel(),
                            onClick = onAdvance,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        Text(
                            text = AppStrings.Abnormal.TraceabilityHint,
                            style = MaterialTheme.typography.caption,
                            color = NeutralTextMuted,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun JidokaStepper(step: JidokaStep) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        JidokaStepItem(label = AppStrings.Abnormal.StepDetect, active = step.ordinal >= JidokaStep.DETECT.ordinal)
        JidokaStepItem(label = AppStrings.Abnormal.StepContain, active = step.ordinal >= JidokaStep.CONTAIN.ordinal)
        JidokaStepItem(label = AppStrings.Abnormal.StepClose, active = step.ordinal >= JidokaStep.CLOSE.ordinal)
    }
}

@Composable
private fun JidokaStepItem(
    label: String,
    active: Boolean,
) {
    val accent = if (active) StatusSuccess else NeutralBorder
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(10.dp)
                    .background(accent, CircleShape)
                    .border(1.dp, accent, CircleShape),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.caption,
            color = if (active) NeutralText else NeutralTextMuted,
        )
    }
}

@Composable
private fun TicketDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = NeutralTextMuted,
            modifier = Modifier.size(14.dp).padding(top = 2.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.caption,
                color = NeutralTextMuted,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.body2,
                color = NeutralText,
            )
        }
    }
}

private fun createDummyTicket(
    now: LocalDateTime,
    index: Int,
): AbnormalTicketUi {
    val timestamp = formatTicketTimestamp(now)
    return AbnormalTicketUi(
        id = "ABN-$index",
        title = AppStrings.Abnormal.ticketTitle(index),
        lineName = if (index % 2 == 0) "Sewing" else "Press",
        problemSummary = AppStrings.Abnormal.DummyProblemSummary,
        createdAt = timestamp,
        updatedAt = timestamp,
        step = JidokaStep.DETECT,
    )
}

private fun JidokaStep.next(): JidokaStep =
    when (this) {
        JidokaStep.DETECT -> JidokaStep.CONTAIN
        JidokaStep.CONTAIN -> JidokaStep.CLOSE
        JidokaStep.CLOSE -> JidokaStep.CLOSE
    }

private fun JidokaStep.progressValue(): Float =
    when (this) {
        JidokaStep.DETECT -> 0.33f
        JidokaStep.CONTAIN -> 0.66f
        JidokaStep.CLOSE -> 1f
    }

private fun JidokaStep.badgeLabel(): String =
    when (this) {
        JidokaStep.DETECT -> AppStrings.Abnormal.StepDetect
        JidokaStep.CONTAIN -> AppStrings.Abnormal.StepContain
        JidokaStep.CLOSE -> AppStrings.Abnormal.StepClose
    }

private fun JidokaStep.badgeColor(): androidx.compose.ui.graphics.Color =
    when (this) {
        JidokaStep.DETECT -> StatusError
        JidokaStep.CONTAIN -> StatusWarning
        JidokaStep.CLOSE -> StatusSuccess
    }

private fun JidokaStep.nextActionLabel(): String =
    when (this) {
        JidokaStep.DETECT -> AppStrings.Abnormal.MarkContain
        JidokaStep.CONTAIN -> AppStrings.Abnormal.MarkClose
        JidokaStep.CLOSE -> AppStrings.Common.Close
    }

private fun formatTicketTimestamp(now: LocalDateTime): String =
    now.format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"))
